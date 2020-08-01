package jaaska.jaakko.photosapp.server.repository

import jaaska.jaakko.photosapp.server.database.MediaDatabase
import jaaska.jaakko.photosapp.server.filesystem.FileSystemScanner
import jaaska.jaakko.photosapp.server.model.MediaMeta

class MediaRepository (private val db: MediaDatabase, private val fsScanner: FileSystemScanner) {

    fun getAllMediaMeta(): List<MediaMeta> = db.getMediaMetas()
    fun getMediaForId(id: Int): MediaMeta? = db.getMediaMeta(id)

    fun rescanLibrary() {
        fsScanner.scanForMedia()
    }

}