package jaaska.jaakko.photosapp.server.processor

import jaaska.jaakko.photosapp.server.model.MediaMeta

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

        // Extract datetimeoriginal
        val dateTimeOriginal = exifProcessor.getDateTimeOriginal(mediaMeta)
        mediaMeta.dateTimeOriginal = dateTimeOriginal ?: "N/A"
    }

}