package jaaska.jaakko.photosapp.server.processor

import jaaska.jaakko.photosapp.server.Logger
import jaaska.jaakko.photosapp.server.configuration.Config
import jaaska.jaakko.photosapp.server.extension.OS_PATH_SEPARATOR
import jaaska.jaakko.photosapp.server.extension.absoluteFilePath
import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.model.MediaType
import net.coobird.thumbnailator.Thumbnails
import org.jcodec.api.FrameGrab
import org.jcodec.common.model.Picture
import org.jcodec.scale.AWTUtil
import java.io.File

class ThumbnailGenerator(config: Config) {

    private val thumbnailDirPath: String = "${config.metaRootPath}${OS_PATH_SEPARATOR}thumbs"

    fun generateThumbnail(mediaMeta: MediaMeta, exifOrientation: Int?) {
        require(mediaMeta.id > 0) { "MediaMeta must have a DB ID before it can be processed!" }

        createThumbnailDirIfNeeded()

        when (mediaMeta.type) {
            MediaType.Picture -> generatePictureThumbnail(mediaMeta, exifOrientation)
            MediaType.Video -> generateVideoThumbnail(mediaMeta)
        }
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

    private fun generatePictureThumbnail(mediaMeta: MediaMeta, exifOrientation: Int?) {
        val mediaFile = File(mediaMeta.absoluteFilePath)
        buildThumbnail(Thumbnails.of(mediaFile), mediaMeta.thumbnailFile(), exifOrientation)
    }

    private fun generateVideoThumbnail(mediaMeta: MediaMeta) {
        val mediaFile = File(mediaMeta.absoluteFilePath)
        val picture = getFrameFromVideoFileOrNull(mediaFile, 10) ?: getFrameFromVideoFileOrNull(mediaFile, 1)

        picture?.also {
            val bufferedImage = AWTUtil.toBufferedImage(it)
            buildThumbnail(Thumbnails.of(bufferedImage), mediaMeta.thumbnailFile())
        }
    }

    private fun getFrameFromVideoFileOrNull(videoFile: File, frameNumber: Int): Picture? =
        try {
            FrameGrab.getFrameFromFile(videoFile, 10)
        } catch (t: Throwable) {
            Logger.e("Video frame (frame $frameNumber) capture failed for `${videoFile.absolutePath}`: ", t)
            null
        }

    private fun <T> buildThumbnail(
        baseBuilder: Thumbnails.Builder<T>,
        thumbnailFile: File,
        exifOrientation: Int? = null
    ) {
        baseBuilder
            .size(256, 256)
            .outputFormat("png")
            .rotate(exifRotationToDegree(exifOrientation))
            .toFile(thumbnailFile)
    }

    private fun MediaMeta.thumbnailFile(): File =
        File("$thumbnailDirPath${OS_PATH_SEPARATOR}${this.id}")
}
