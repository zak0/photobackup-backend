package jaaska.jaakko.photosapp.server.repository

import jaaska.jaakko.photosapp.server.BuildConfig
import jaaska.jaakko.photosapp.server.database.KeyValueStore
import jaaska.jaakko.photosapp.server.model.ServerInfo
import kotlin.random.Random

class ServerInfoRepository(
    private val keyValueStore: KeyValueStore
) {

    private companion object {
        const val KEY_SERVER_ID = "serverId"
        const val KEY_SERVER_NAME = "serverName"
    }

    /**
     * Initializes server information into the database, if it has not been initialized yet. This should only be the
     * case on first (successful) launch of the server.
     *
     * Whether the server info has been initialized or not, is based on existence of server ID in the database.
     */
    fun initIfNeeded() {
        if (!keyValueStore.contains("serverId")) {
            // Server ID is a random 64bit number, represented as HEX string
            val serverId = Random(System.currentTimeMillis()).nextLong().toString(16)
            keyValueStore.put(KEY_SERVER_ID, serverId)

            // Initial server name (this is changeable by the user later on) is first four characters of the server ID
            val serverName = serverId.substring(0, 4)
            keyValueStore.put(KEY_SERVER_NAME, serverName)
        }
    }

    fun getServerInfo(): ServerInfo {
        return ServerInfo(
            keyValueStore.get(KEY_SERVER_ID) ?: error("Server info is not initialized!"),
            keyValueStore.get(KEY_SERVER_NAME) ?: error("Server info is not initialized!"),
            BuildConfig.versionName,
            BuildConfig.versionCode // TODO
        )
    }
}