package jaaska.jaakko.photosapp.server.filesystem

import jaaska.jaakko.photosapp.server.Logger
import jaaska.jaakko.photosapp.server.database.MediaDatabase
import jaaska.jaakko.photosapp.server.extension.isSupportedImageFile
import jaaska.jaakko.photosapp.server.extension.md5String
import jaaska.jaakko.photosapp.server.model.MediaMeta
import java.io.File

/**
 *  Class for performing file-level filesystem operations.
 *
 *  Object is initialized with [mediaRoots] list of absolute paths to root directories with media to include, and
 *  [metaRoot] absolute path to the directory containing library meta data.
 */
class FileSystemScanner(private val mediaRoots: List<String>, private val metaRoot: String, private val db: MediaDatabase) {

    val mediaMetas = HashMap<String, MediaMeta>()

    fun scanForMedia() {
        mediaRoots.forEach { scanForMedia(it) }
    }

    private fun scanForMedia(rootDirPath: String) {
        val root = File(rootDirPath)

        if (root.exists() && root.isDirectory) {
            root.listFiles()?.forEach { file ->
                // Recurse into subdirectories
                if (file.isDirectory) {
                    scanForMedia(file.absolutePath)
                }

                // Handle supported media files
                if (file.isSupportedImageFile) {
                    Logger.d("${file.absolutePath} - ${file.md5String} - ${file.length()}")
                    handleImageFile(file)
                }
            }
        }
    }

    private fun handleImageFile(imageFile: File) {
        val meta = MediaMeta(
            -1,
            imageFile.name,
            imageFile.length(),
            imageFile.parent,
            imageFile.md5String,
            "N/A", // TODO Add an actual date here
            "processing" // TODO Change to "processing"
        )

        db.persistMediaMeta(meta)
    }

}
