package jaaska.jaakko.photosapp.server.extension

import jaaska.jaakko.photosapp.server.Constants
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

val File.isSupportedImageFile: Boolean
    get() {
        if (!isFile) {
            return false
        }

        val fileExtension = absolutePath.split(".").lastOrNull() ?: ""
        return Constants.SUPPORTED_IMAGE_FILE_EXTENSIONS.contains(fileExtension.toLowerCase())
    }

val File.md5String: String
    get() {
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(32768)
        val stream = inputStream()

        var readBytes = stream.read(buffer)
        while (readBytes > 0) {
            digest.update(buffer, 0, readBytes)
            readBytes = stream.read(buffer)
        }

        val md5Sum = digest.digest()
        val bigInt = BigInteger(1, md5Sum)
        val asString = bigInt.toString(16)

        return String.format("%32s", asString).replace(" ", "0")
    }