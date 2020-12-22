package jaaska.jaakko.photosapp.server.processor

import jaaska.jaakko.photosapp.server.Logger
import jaaska.jaakko.photosapp.server.extension.OS_PATH_SEPARATOR
import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.model.MediaStatus
import jaaska.jaakko.photosapp.server.model.MediaType
import jaaska.jaakko.photosapp.server.util.DateUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

class MediaProcessor(private val thumbnailGenerator: ThumbnailGenerator, private val exifProcessor: ExifProcessor) {

    /**
     * Processes the media file pointed by [mediaMeta].
     *
     * Note! Given [MediaMeta] has to have a database ID at this point!!
     */
    fun processMedia(mediaMeta: MediaMeta) {
        require(mediaMeta.id > 0) { "MediaMeta must have a DB ID before it can be processed!" }

        when (mediaMeta.type) {
            MediaType.Picture -> processPicture(mediaMeta)
            MediaType.Video -> processVideo(mediaMeta)
        }

        // Finally mark media as ready
        //mediaMeta.status = MediaStatus.READY
    }

    private fun processPicture(mediaMeta: MediaMeta) {
        val orientation = exifProcessor.getOrientation(mediaMeta)

        // Generate thumbnail
        thumbnailGenerator.generateThumbnailForMedia(mediaMeta, orientation)

        // Extract datetimeoriginal either from EXIF, or if not available, fall back to file timestamps.
        val dateTimeOriginal = exifProcessor.getDateTimeOriginal(mediaMeta) ?: getTimeFromFileTimestamps(mediaMeta)

        mediaMeta.dateTimeOriginal = dateTimeOriginal
        mediaMeta.status = MediaStatus.READY
    }

    private fun processVideo(mediaMeta: MediaMeta) {
        mediaMeta.dateTimeOriginal = getTimeFromFileTimestamps(mediaMeta)
    }

    /**
     * Attempts to read timestamp of last modification of given [MediaMeta]. If that fails, tries to read file creation
     * timestamp.
     *
     * NOTE! File creation time (as per its documentation) relies on filesystem support for that file attribute.
     * If this attribute is not available, creation time will be "system default", usually Epoch 0.
     */
    private fun getTimeFromFileTimestamps(mediaMeta: MediaMeta): String {
        val filePathString = "${mediaMeta.dirPath}${OS_PATH_SEPARATOR}${mediaMeta.fileName}"
        val filePath = File(filePathString).toPath()
        val fileAttributes = Files.readAttributes(filePath, BasicFileAttributes::class.java)
        val creationTime = fileAttributes.creationTime().toMillis()
        val modifiedTime = fileAttributes.lastModifiedTime().toMillis()

        return DateUtil.dateToExifDateTime(Date(if (modifiedTime != 0L) modifiedTime else creationTime))
    }
}