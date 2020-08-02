package jaaska.jaakko.photosapp.server

import jaaska.jaakko.photosapp.server.database.MediaDatabase
import jaaska.jaakko.photosapp.server.database.SqliteMediaDatabase
import jaaska.jaakko.photosapp.server.filesystem.FileSystemScanner
import jaaska.jaakko.photosapp.server.processor.ExifProcessor
import jaaska.jaakko.photosapp.server.processor.MediaProcessor
import jaaska.jaakko.photosapp.server.processor.ThumbnailGenerator
import jaaska.jaakko.photosapp.server.repository.MediaRepository

/**
 * Pure DI "composition root".
 */
class ModuleProvider() {

    private val mediaRoots = listOf("C:\\Users\\jaakko\\Temp\\photobackup-media")
    val metaRoot = "C:\\Users\\jaakko\\Temp\\photobackup-meta"

    private val mediaDatabase: MediaDatabase by lazy { SqliteMediaDatabase(metaRoot) }
    private val fileSystemScanner: FileSystemScanner by lazy { FileSystemScanner(mediaRoots, metaRoot, mediaDatabase) }
    private val thumbnailGenerator: ThumbnailGenerator by lazy { ThumbnailGenerator(metaRoot) }
    private val exifProcessor: ExifProcessor by lazy { ExifProcessor() }
    private val mediaProcessor: MediaProcessor by lazy { MediaProcessor(thumbnailGenerator, exifProcessor) }

    val mediaRepository: MediaRepository by lazy { MediaRepository(mediaDatabase, fileSystemScanner, metaRoot, mediaProcessor) }

}