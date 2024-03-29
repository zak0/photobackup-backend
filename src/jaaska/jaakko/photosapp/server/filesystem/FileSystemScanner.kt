package jaaska.jaakko.photosapp.server.filesystem

import jaaska.jaakko.photosapp.server.Logger
import jaaska.jaakko.photosapp.server.configuration.Config
import jaaska.jaakko.photosapp.server.extension.isSupportedImageFile
import jaaska.jaakko.photosapp.server.extension.isSupportedVideoFile
import jaaska.jaakko.photosapp.server.extension.md5String
import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.model.MediaType
import java.io.File

/**
 *  Class for performing file-level filesystem operations.
 *
 *  Object is initialized with [mediaRoots] list of absolute paths to root directories with media to include, and
 *  [metaRoot] absolute path to the directory containing library meta data.
 */
class FileSystemScanner(config: Config) {

    private val mediaRoots: List<String> = ArrayList(config.mediaDirs).apply { add(config.uploadsDir) }
    private val metaRoot: String = config.metaRootPath

    fun scanForMedia(onMediaFile: (MediaMeta) -> Unit) {
        mediaRoots.forEach { scanForMedia(it, onMediaFile) }
    }

    private fun scanForMedia(rootDirPath: String, onMediaFile: (MediaMeta) -> Unit) {
        val root = File(rootDirPath)

        if (root.exists() && root.isDirectory) {
            root.listFiles()?.forEach { file ->
                // Recurse into subdirectories
                if (file.isDirectory) {
                    scanForMedia(file.absolutePath, onMediaFile)
                }

                // Handle supported media files
                if (file.isSupportedImageFile) {
                    Logger.d("Picture - ${file.absolutePath} - ${file.md5String} - ${file.length()}")
                    handleImageFile(file, onMediaFile)
                } else if (file.isSupportedVideoFile) {
                    Logger.d("Video - ${file.absolutePath} - ${file.md5String} - ${file.length()}")
                    handleVideoFile(file, onMediaFile)
                }
            }
        }
    }

    private fun handleImageFile(imageFile: File, onMediaFile: (MediaMeta) -> Unit) {
        val meta = MediaMeta(
            -1,
            MediaType.Picture,
            imageFile.name,
            imageFile.length(),
            imageFile.parent,
            imageFile.md5String,
            "N/A", // Date will be added during processing
            "unknown" // Status will be cleared out later on
        )

        onMediaFile(meta)
    }

    private fun handleVideoFile(videoFile: File, onMediaFile: (MediaMeta) -> Unit) {
        val meta = MediaMeta(
            -1,
            MediaType.Video,
            videoFile.name,
            videoFile.length(),
            videoFile.parent,
            videoFile.md5String,
            "N/A", // Date will be added during processing
            "unknown" // Status will be cleared out later on
        )

        onMediaFile(meta)
    }

}
