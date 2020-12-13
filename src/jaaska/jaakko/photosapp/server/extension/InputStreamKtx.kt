package jaaska.jaakko.photosapp.server.extension

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.StringBuilder

/**
 * Copies this [InputStream] to [out] [OutputStream].
 *
 * Shamelessly copied from an example code found on https://ktor.io/servers/uploads.html
 */
suspend fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    yieldSize: Int = 4 * 1024 * 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Long {
    return withContext(dispatcher) {
        val buffer = ByteArray(bufferSize)
        var bytesCopied = 0L
        var bytesAfterYield = 0L
        while (true) {
            val bytes = read(buffer).takeIf { it >= 0 } ?: break
            out.write(buffer, 0, bytes)
            if (bytesAfterYield >= yieldSize) {
                yield()
                bytesAfterYield %= yieldSize
            }
            bytesCopied += bytes
            bytesAfterYield += bytes
        }
        return@withContext bytesCopied
    }
}

/**
 * Reads an [InputStream] of characters and compiles them into a [String].
 */
fun InputStream.asString(): String {
    val output = StringBuilder()
    val reader = BufferedReader(InputStreamReader(this))
    var line = reader.readLine()
    while (line != null) {
        output.append(line)
        line = reader.readLine()
    }
    reader.close()
    return output.toString()
}
