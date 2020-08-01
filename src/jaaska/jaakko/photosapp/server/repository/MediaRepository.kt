package jaaska.jaakko.photosapp.server.repository

import jaaska.jaakko.photosapp.server.database.MediaDatabase
import jaaska.jaakko.photosapp.server.filesystem.FileSystemScanner

class MediaRepository (private val db: MediaDatabase, private val fsScanner: FileSystemScanner) {

    fun rescanLibrary() {
        fsScanner.scanForMedia()
    }

}