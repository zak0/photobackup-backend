package jaaska.jaakko.photosapp.server.processor

import jaaska.jaakko.photosapp.server.configuration.Config
import jaaska.jaakko.photosapp.server.extension.OS_PATH_SEPARATOR
import jaaska.jaakko.photosapp.server.extension.absoluteFilePath
import jaaska.jaakko.photosapp.server.model.MediaMeta
import net.coobird.thumbnailator.Thumbnails
import java.io.File

class ThumbnailGenerator(config: Config) {

    private val thumbnailDirPath: String = "${config.metaRootPath}${OS_PATH_SEPARATOR}thumbs"

    fun generateThumbnailForMedia(mediaMeta: MediaMeta, exifOrientation: Int?) {
        require(mediaMeta.id > 0) { "MediaMeta must have a DB ID before it can be processed!" }

        createThumbnailDirIfNeeded()

        val mediaFile = File(mediaMeta.absoluteFilePath)
        val thumbnailFile = File("$thumbnailDirPath${OS_PATH_SEPARATOR}${mediaMeta.id}")

        Thumbnails.of(mediaFile)
            .size(256, 256)
            .outputFormat("png")
            .rotate(exifRotationToDegree(exifOrientation))
            .toFile(thumbnailFile)
    }

    /**
     * Converts EXIF rotation value into degrees, that the image needs to be rotated in order to appear "the right
     * way up".
     */
    private fun exifRotationToDegree(exifRotation: Int?): Double {
        return when (exifRotation) {
            8 -> -90.0
            3 -> 180.0
            6 -> 90.0
            else -> 0.0
        }
    }

    private fun createThumbnailDirIfNeeded() {
        val thumbnailDir = File(thumbnailDirPath)
        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdir()
        }
    }

}
