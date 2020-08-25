package jaaska.jaakko.photosapp.server.configuration

import jaaska.jaakko.photosapp.server.Logger
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

@UnstableDefault
class ConfigLoader {

    lateinit var config: Config

    fun loadConfig(configFilePath: String): Config? {

        // Read config JSON file
        val file = File(configFilePath)
        if (!file.exists()) {
            Logger.e("Config file '$configFilePath' not found.")
            return null
        }

        val asString = String(file.inputStream().readAllBytes())

        // Parse config
        val config = try {
            parseConfig(asString)
        } catch (e: Exception) {
            Logger.e("Config file parsing failed: ", e)
            null
        }

        // Validate config
        val isValid = config?.let { configIsValid(it) } ?: false

        // Return Config when all good, null otherwise
        return if (isValid) config else null
    }

    private fun parseConfig(jsonString: String): Config {
        val root = Json.parseJson(jsonString).jsonObject
        val metaRootPath = root["metaRootPath"]!!.content
        val mediaDirs = ArrayList<String>()
        root["mediaDirs"]!!.jsonArray.forEach { mediaDirs.add(it.content) }
        val uploadsDir = root["uploadsDir"]!!.content
        val serverPort = root["serverPort"]!!.int

        return Config(
            metaRootPath,
            mediaDirs,
            uploadsDir,
            serverPort
        )
    }

    private fun configIsValid(config: Config): Boolean {
        // Check that directories exist
        val metaDirExists = File(config.metaRootPath).exists()
        val mediaDirsExist = config.mediaDirs.count { File(it).exists() } == config.mediaDirs.size
        val uploadsDirExists = File(config.uploadsDir).exists()

        // Check that server port is sensible
        val portIsSensible = config.serverPort in 2..65535

        return metaDirExists && mediaDirsExist && uploadsDirExists && portIsSensible
    }

}