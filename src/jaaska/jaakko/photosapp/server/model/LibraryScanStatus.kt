package jaaska.jaakko.photosapp.server.model

import kotlinx.serialization.Serializable

@Serializable
data class LibraryScanStatus(
    val state: LibraryScanState,
    val mediaFilesDetected: Int = 0,
    val filesMoved: Int = 0,
    val filesRemoved: Int = 0,
    val newFiles: Int = 0,
    val filesToProcess: Int = 0,
    val filesProcessed: Int = 0
)

/**
 * DO NOT CHANGE THESE VALUES, THIS WILL BREAK CLIENTS AS NAMES OF VALUES ARE PASSED IN /scanstatus RESPONSES!!
 */
enum class LibraryScanState {
    SCANNING_FOR_FILES,
    PROCESSING_FILES,
    DONE
}
