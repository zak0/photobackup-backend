package jaaska.jaakko.photosapp.server.repository

import jaaska.jaakko.photosapp.server.Logger
import jaaska.jaakko.photosapp.server.database.MediaDatabase
import jaaska.jaakko.photosapp.server.extension.OS_PATH_SEPARATOR
import jaaska.jaakko.photosapp.server.extension.md5String
import jaaska.jaakko.photosapp.server.filesystem.FileSystemScanner
import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.model.MediaStatus
import jaaska.jaakko.photosapp.server.processor.MediaProcessor
import java.io.File

class MediaRepository(
    private val db: MediaDatabase,
    private val fsScanner: FileSystemScanner,
    private val metaRoot: String,
    private val uploadsDir: String,
    private val mediaProcessor: MediaProcessor
) {

    /**
     * Map of [MediaMeta]s, [MediaMeta.id] to [MediaMeta].
     */
    private val mediaMetasCache = HashMap<Int, MediaMeta>()

    /**
     * Map of [MediaMeta]s, [MediaMeta.checksum] to [MediaMeta].
     */
    private val mediaMetasByHash = HashMap<String, MediaMeta>()

    fun getAllMediaMeta(): List<MediaMeta> {
        initCachesIfNeeded()
        return mediaMetasCache.values.toList()
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
     * Populates remaining fields with proper values, then persists it into the database.
     */
    fun onMediaMetaReceived(mediaMeta: MediaMeta) {
        mediaMeta.id = -1
        mediaMeta.status = MediaStatus.UPLOAD_PENDING
        mediaMeta.dirPath = uploadsDir
        db.persistMediaMeta(mediaMeta)
        cacheMedia(mediaMeta)
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
    }

    /**
     * Checks if [mediaFile] matches the metadata defined in [mediaMeta].
     */
    fun mediaMatchesMeta(mediaMeta: MediaMeta, mediaFile: File): Boolean {
        return mediaFile.length() == mediaMeta.fileSize && mediaFile.md5String == mediaMeta.checksum
    }

    fun rescanLibrary() {
        initCachesIfNeeded()

        // Entries from this map will be removed as existing files are discovered.
        // After processing, entries left in this map are files that no longer exist.
        val removedFiles = HashMap<String, MediaMeta>(mediaMetasByHash)

        var existingFiles = 0
        var filesMoved = 0
        var newFiles = 0
        var filesRemoved = 0

        fsScanner.scanForMedia() { meta ->
            if (!mediaMetasByHash.containsKey(meta.checksum)) {
                // This is a new file
                newFiles++

                // Add it to DB with status "processing"
                meta.status = "processing"
                db.persistMediaMeta(meta)

                // Add it to caches
                cacheMedia(meta)

                // Process it
                mediaProcessor.processMedia(meta)
                meta.status = "ready"

                // Update state after processing
                db.persistMediaMeta(meta)
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
        }

        // Delete removed files from the database
        filesRemoved = removedFiles.size
        removedFiles.values.forEach { removedMeta ->
            unCacheMedia(removedMeta)
            db.deleteMediaMeta(removedMeta)
        }

        Logger.i("Library scan complete. New files: $newFiles, moved/renamed files: $filesMoved, removed files: $filesRemoved, existing files: $existingFiles")
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