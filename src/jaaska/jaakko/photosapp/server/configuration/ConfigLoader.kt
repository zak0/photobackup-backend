package jaaska.jaakko.photosapp.server.configuration

import jaaska.jaakko.photosapp.server.Logger
import jaaska.jaakko.photosapp.server.extension.asString
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

class ConfigLoader {

    lateinit var config: Config

    fun loadConfig(configFilePath: String): Config? {

        // Read config JSON file
        val file = File(configFilePath)
        if (!file.exists()) {
            Logger.e("Config file '$configFilePath' not found.")
            return null
        }

        val asString = file.inputStream().asString()

        // Parse config
        val config = try {
            Json.decodeFromString<Config>(asString)
        } catch (e: Exception) {
            Logger.e("Config file parsing failed: ", e)
            null
        }

        // Validate config
        val isValid = config?.let { configIsValid(it) } ?: false

        if (isValid) {
            this.config = config!!
        }

        // Return Config when all good, null otherwise
        return if (isValid) config else null
    }

    private fun configIsValid(config: Config): Boolean {
        // Initial admin password must not be empty
        val initialAdminPwOk = config.initialAdminPassword.isNotBlank()

        // Check that directories exist
        val metaDirExists = File(config.metaRootPath).exists()
        val mediaDirsExist = config.mediaDirs.count { File(it).exists() } == config.mediaDirs.size
        val uploadsDirExists = File(config.uploadsDir).exists()

        // Check that server port is sensible
        val portIsSensible = config.serverPort in 2..65535

        return metaDirExists && mediaDirsExist && uploadsDirExists && portIsSensible && initialAdminPwOk
    }

}