package jaaska.jaakko.photosapp.server.repository

import jaaska.jaakko.photosapp.server.Logger
import jaaska.jaakko.photosapp.server.configuration.Config
import jaaska.jaakko.photosapp.server.database.MediaDatabase
import jaaska.jaakko.photosapp.server.extension.OS_PATH_SEPARATOR
import jaaska.jaakko.photosapp.server.extension.md5String
import jaaska.jaakko.photosapp.server.filesystem.FileSystemScanner
import jaaska.jaakko.photosapp.server.model.LibraryScanState
import jaaska.jaakko.photosapp.server.model.LibraryScanStatus
import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.model.MediaStatus
import jaaska.jaakko.photosapp.server.processor.MediaProcessor
import kotlinx.coroutines.*
import java.io.File

class MediaRepository(
    private val db: MediaDatabase,
    private val fsScanner: FileSystemScanner,
    config: Config,
    private val mediaProcessor: MediaProcessor
) {

    private val metaRoot = config.metaRootPath
    private val uploadsDir = config.uploadsDir

    private var libraryScanJob: Job? = null

    @Volatile
    var libraryScanStatus: LibraryScanStatus? = null

    /**
     * Map of [MediaMeta]s, [MediaMeta.id] to [MediaMeta].
     */
    private val mediaMetasCache = HashMap<Int, MediaMeta>()

    /**
     * Map of [MediaMeta]s, [MediaMeta.checksum] to [MediaMeta].
     */
    private val mediaMetasByHash = HashMap<String, MediaMeta>()

    fun getAllMediaMeta(limit: Int = Int.MAX_VALUE, offset: Int = 0): List<MediaMeta> {
        initCachesIfNeeded()

        val metas = mediaMetasCache.values.toList()

        return if (limit < metas.size) {
            val safeFromIndex = minOf(offset, metas.size - 1)
            val safeToIndex = minOf(safeFromIndex + limit, metas.size)

            mediaMetasCache.values.toList().subList(safeFromIndex, safeToIndex)
        } else {
            mediaMetasCache.values.toList()
        }
    }

    fun getMediaForId(id: Int): MediaMeta? {
        initCachesIfNeeded()
        return mediaMetasCache[id]
    }

    fun getThumbnailPath(mediaMeta: MediaMeta): String =
        "${metaRoot}${OS_PATH_SEPARATOR}thumbs${OS_PATH_SEPARATOR}${mediaMeta.id}.png"

    /**
     * Handler for when a client POSTs a new media metadata.
     *
     * If media already exists (size and checksum) match an existing meta data, then returns that existing meta data.
     * If this is a new media item, populates remaining fields with proper values, then persists it into the database.
     *
     * @return [Pair] where first value is a boolean telling if new media meta was persisted into the db. When [Boolean]
     *  is true, the second value [MediaMeta] is the [newMeta] with necessary fields populated. If it's false, then an
     *  existing [MediaMeta] object is in the second field of the [Pair].
     */
    fun onMediaMetaReceived(newMeta: MediaMeta): Pair<Boolean, MediaMeta> {
        initCachesIfNeeded()

        return mediaMetasByHash[newMeta.checksum]?.let { existingMeta ->
            Pair(false, existingMeta)
        } ?: let {
            newMeta.id = -1
            newMeta.status = MediaStatus.UPLOAD_PENDING
            newMeta.dirPath = uploadsDir
            db.persistMediaMeta(newMeta)
            cacheMedia(newMeta)
            Pair(true, newMeta)
        }
    }

    /**
     * Handler for when a client uploads a media file.
     *
     * Changes the status of the meta data to "processing" and persist it.
     * Then passes it on to [MediaProcessor] for processing.
     */
    fun onMediaFileReceived(mediaMeta: MediaMeta) {
        mediaMeta.status = MediaStatus.PROCESSING
        mediaProcessor.processMedia(mediaMeta)
        db.persistMediaMeta(mediaMeta)
    }

    /**
     * Checks if [mediaFile] matches the metadata defined in [mediaMeta].
     */
    fun mediaMatchesMeta(mediaMeta: MediaMeta, mediaFile: File): Boolean {
        return mediaFile.length() == mediaMeta.fileSize &&
                mediaFile.md5String.equals(
                    mediaMeta.checksum,
                    ignoreCase = true
                )
    }

    /**
     * Starts a media library scans for media files. Scan is started as a background coroutine.
     *
     * @return true when when scan was started, false if not (e.g. when a scan was already running)
     */
    fun scanLibrary(): Boolean {
        if (libraryScanJob?.isActive == true) {
            return false
        } else {
            libraryScanJob = GlobalScope.launch {
                initCachesIfNeeded()

                // Entries from this map will be removed as existing files are discovered.
                // After processing, entries left in this map are files that no longer exist.
                val removedFiles = HashMap<String, MediaMeta>(mediaMetasByHash)

                var detectedFiles = 0
                var existingFiles = 0
                var filesMoved = 0
                var newFiles = 0
                var filesRemoved = 0

                libraryScanStatus = LibraryScanStatus(LibraryScanState.SCANNING_FOR_FILES)

                fsScanner.scanForMedia() { meta ->
                    detectedFiles++

                    if (!mediaMetasByHash.containsKey(meta.checksum)) {
                        // This is a new file
                        newFiles++

                        // Add it to DB with status "processing"
                        meta.status = "processing"
                        db.persistMediaMeta(meta)

                        // Add it to caches
                        cacheMedia(meta)
                    } else {
                        // This file existed.
                        val existingMeta = mediaMetasByHash[meta.checksum]!!
                        if (meta.dirPath != existingMeta.dirPath || meta.fileName != existingMeta.fileName) {
                            // If the newly found file is e.g. in different folder now, update the record
                            filesMoved++
                            existingMeta.dirPath = meta.dirPath
                            existingMeta.fileName = meta.fileName
                            db.persistMediaMeta(existingMeta)
                        } else {
                            // The file is still the same
                            existingFiles++
                        }

                        removedFiles.remove(meta.checksum)
                    }

                    libraryScanStatus = libraryScanStatus?.copy(
                        mediaFilesDetected = detectedFiles,
                        filesMoved = filesMoved,
                        newFiles = newFiles
                    )
                }

                // Delete removed files from the database
                filesRemoved = removedFiles.size
                removedFiles.values.forEach { removedMeta ->
                    unCacheMedia(removedMeta)
                    db.deleteMediaMeta(removedMeta)
                }

                // After scan is complete, we have total numbers of removed files.
                libraryScanStatus =
                    libraryScanStatus?.copy(state = LibraryScanState.PROCESSING_FILES, filesRemoved = filesRemoved)

                // Process files that are pending processing
                val filesToProcess = mediaMetasCache.values.filter { it.status == MediaStatus.PROCESSING }
                var filesProcessed = 0
                libraryScanStatus = libraryScanStatus?.copy(filesToProcess = filesToProcess.size)
                filesToProcess.forEach { mediaToProcess ->
                    mediaProcessor.processMedia(mediaToProcess)
                    db.persistMediaMeta(mediaToProcess)
                    filesProcessed++
                    libraryScanStatus = libraryScanStatus?.copy(filesProcessed = filesProcessed)
                }

                libraryScanStatus = libraryScanStatus?.copy(state = LibraryScanState.DONE)
                Logger.i("Library scan complete. New files: $newFiles, moved/renamed files: $filesMoved, removed files: $filesRemoved, existing files: $existingFiles")
            }

            return true
        }
    }

    private fun unCacheMedia(mediaMeta: MediaMeta) {
        if (mediaMeta.id > 0) {
            mediaMetasCache.remove(mediaMeta.id)
            mediaMetasByHash.remove(mediaMeta.checksum)
        }
    }

    private fun cacheMedia(mediaMeta: MediaMeta) {
        if (mediaMeta.id > 0) {
            mediaMetasCache[mediaMeta.id] = mediaMeta
            mediaMetasByHash[mediaMeta.checksum] = mediaMeta
        }
    }

    private fun initCachesIfNeeded() {
        if (mediaMetasCache.isEmpty()) {
            Logger.d("Loading media metadata into cache...")
            mediaMetasCache.putAll(db.getMediaMetas().map { it.id to it })
            mediaMetasByHash.putAll(mediaMetasCache.values.map { it.checksum to it })
        }
    }

}