package jaaska.jaakko.photosapp.server.extension

import java.math.BigInteger
import java.security.MessageDigest
import java.util.regex.Pattern

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(this.toByteArray())).toString(16).padStart(32, '0')
}

/**
 * Matches [pattern] to [this] and returns first match. If no match is found, returns null.
 */
fun String.regexSubstring(pattern: String): String? {
    return Pattern.compile(pattern).matcher(this).takeIf { it.find() }?.group(0)
}
