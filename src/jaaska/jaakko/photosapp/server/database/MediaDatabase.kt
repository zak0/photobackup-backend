package jaaska.jaakko.photosapp.server.database

import jaaska.jaakko.photosapp.server.model.MediaMeta

interface MediaDatabase {

    fun getMediaMetas(): List<MediaMeta>
    fun persistMediaMeta(mediaMeta: MediaMeta)
    fun deleteMediaMeta(mediaMeta: MediaMeta)

}