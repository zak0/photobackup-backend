package jaaska.jaakko.photosapp.server

import jaaska.jaakko.photosapp.server.database.MediaDatabase
import jaaska.jaakko.photosapp.server.database.SqliteMediaDatabase
import jaaska.jaakko.photosapp.server.filesystem.FileSystemScanner
import jaaska.jaakko.photosapp.server.repository.MediaRepository

/**
 * Pure DI "composition root".
 */
class ModuleProvider() {

    private val mediaRoots = listOf("C:\\Users\\jaakko\\Temp\\photobackup-media")
    private val metaRoot = "C:\\Users\\jaakko\\Temp\\photobackup-meta"

    private val mediaDatabase: MediaDatabase by lazy { SqliteMediaDatabase(metaRoot) }
    private val fileSystemScanner: FileSystemScanner by lazy { FileSystemScanner(mediaRoots, metaRoot, mediaDatabase) }

    val mediaRepository: MediaRepository by lazy { MediaRepository(mediaDatabase, fileSystemScanner) }

}