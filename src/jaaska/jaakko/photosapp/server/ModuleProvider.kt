package jaaska.jaakko.photosapp.server

import jaaska.jaakko.photosapp.server.configuration.Config
import jaaska.jaakko.photosapp.server.configuration.ConfigLoader
import jaaska.jaakko.photosapp.server.database.*
import jaaska.jaakko.photosapp.server.filesystem.FileSystemScanner
import jaaska.jaakko.photosapp.server.processor.ExifProcessor
import jaaska.jaakko.photosapp.server.processor.MediaProcessor
import jaaska.jaakko.photosapp.server.processor.ThumbnailGenerator
import jaaska.jaakko.photosapp.server.repository.MediaRepository
import jaaska.jaakko.photosapp.server.repository.ServerInfoRepository
import jaaska.jaakko.photosapp.server.repository.UsersRepository

/**
 * Pure DI "composition root".
 */
class ModuleProvider() {

    private val metaDatabase by lazy { SqliteMetaDatabase(config) }
    private val mediaDatabase: MediaDatabase by lazy { metaDatabase }
    private val keyValueDatabase: KeyValueDatabase by lazy { metaDatabase }
    private val usersDatabase: UsersDatabase by lazy { metaDatabase }

    private val keyValueStore: KeyValueStore by lazy { KeyValueStore(keyValueDatabase) }

    private val fileSystemScanner: FileSystemScanner by lazy { FileSystemScanner(config) }
    private val thumbnailGenerator: ThumbnailGenerator by lazy { ThumbnailGenerator(config) }
    private val exifProcessor: ExifProcessor by lazy { ExifProcessor() }
    private val mediaProcessor: MediaProcessor by lazy { MediaProcessor(thumbnailGenerator, exifProcessor) }

    val configLoader: ConfigLoader by lazy { ConfigLoader() }

    /** Utility getter for [Config]. [ConfigLoader.loadConfig] must be called before this is available. */
    val config: Config
        get() = configLoader.config

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(
            mediaDatabase,
            fileSystemScanner,
            config,
            mediaProcessor
        )
    }

    val serverInfoRepository: ServerInfoRepository by lazy {
        ServerInfoRepository(
            keyValueStore
        )
    }

    val usersRepository: UsersRepository by lazy {
        UsersRepository(
            usersDatabase,
            config
        )
    }

}