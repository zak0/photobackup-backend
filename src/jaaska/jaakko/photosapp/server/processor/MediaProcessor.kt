package jaaska.jaakko.photosapp.server.processor

import jaaska.jaakko.photosapp.server.extension.OS_PATH_SEPARATOR
import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.model.MediaStatus
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

        // Generate thumbnail
        thumbnailGenerator.generateThumbnailForMedia(mediaMeta)

        // Extract datetimeoriginal either from EXIF, or if not available, try to use file creation time, if that fails
        // try lastModifiedTime.
        // NOTE! File creation time (as per its documentation) relies on filesystem support for that file attribute.
        // If this attribute is not available, creation time will be "system default", usually Epoch 0.
        val dateTimeOriginal = exifProcessor.getDateTimeOriginal(mediaMeta) ?: let {
            // Fall back to file creation time if time is not available through EXIF
            val filePathString = "${mediaMeta.dirPath}${OS_PATH_SEPARATOR}${mediaMeta.fileName}"
            val filePath = File(filePathString).toPath()
            val fileAttributes = Files.readAttributes(filePath, BasicFileAttributes::class.java)
            val creationTime = fileAttributes.creationTime().toMillis()
            val modifiedTime = fileAttributes.lastModifiedTime().toMillis()

            DateUtil.dateToExifDate(Date(if (creationTime != 0L) creationTime else modifiedTime))
        }

        mediaMeta.dateTimeOriginal = dateTimeOriginal

        // Finally mark media as ready
        mediaMeta.status = MediaStatus.READY
    }

}