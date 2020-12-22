package jaaska.jaakko.photosapp.server.processor

import jaaska.jaakko.photosapp.server.configuration.Config
import jaaska.jaakko.photosapp.server.extension.OS_PATH_SEPARATOR
import jaaska.jaakko.photosapp.server.extension.onNoneNull
import jaaska.jaakko.photosapp.server.extension.regexSubstring
import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.model.MediaStatus
import jaaska.jaakko.photosapp.server.model.MediaType
import jaaska.jaakko.photosapp.server.util.DateUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.format.DateTimeFormatter
import java.util.*

class MediaProcessor(
    private val thumbnailGenerator: ThumbnailGenerator,
    private val exifProcessor: ExifProcessor,
    private val config: Config
) {

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
        mediaMeta.status = MediaStatus.READY
    }

    private fun processPicture(mediaMeta: MediaMeta) {
        val orientation = exifProcessor.getOrientation(mediaMeta)

        // Generate thumbnail
        thumbnailGenerator.generateThumbnail(mediaMeta, orientation)

        // Extract datetimeoriginal either from EXIF, or if not available, attempt to parse time from filename, if
        // filename date patterns are defined in Config.
        val dateTimeOriginal = exifProcessor.getDateTimeOriginal(mediaMeta) ?: parseTimeFromFileName(mediaMeta) ?: ""

        mediaMeta.dateTimeOriginal = dateTimeOriginal
    }

    private fun processVideo(mediaMeta: MediaMeta) {
        // For videos, makes a best effort guess from file metadata for capture time.
        mediaMeta.dateTimeOriginal = parseTimeFromFileName(mediaMeta) ?: ""

        // Generate thumbnail
        thumbnailGenerator.generateThumbnail(mediaMeta, null)
    }

    /**
     * Attempts to parse time from file name based on the patterns defined in configuration.
     */
    private fun parseTimeFromFileName(mediaMeta: MediaMeta): String? {
        return onNoneNull(config.filenameDatePattern, config.filenameDateRegex) { pattern, regex ->
            val rawName = mediaMeta.fileName.substringBeforeLast(".")

            val datePart = rawName.regexSubstring(regex)
            val inputFormatter = DateTimeFormatter.ofPattern(pattern)

            try {
                val inputTemporal = inputFormatter.parse(datePart)

                val outputFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                outputFormatter.format(inputTemporal)
            } catch (t: Throwable) {
                null
            }
        }
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