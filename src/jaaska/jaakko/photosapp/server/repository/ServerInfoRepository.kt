package jaaska.jaakko.photosapp.server.repository

import jaaska.jaakko.photosapp.server.configuration.Config
import jaaska.jaakko.photosapp.server.model.ServerInfo

class ServerInfoRepository(
    private val config: Config
) {

    fun getServerInfo(): ServerInfo {
        return ServerInfo(
            "",
            "",
            -1
        )
    }

}