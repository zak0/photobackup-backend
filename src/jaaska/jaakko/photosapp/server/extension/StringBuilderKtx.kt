package jaaska.jaakko.photosapp.server.extension

/**
 * Appends a [String] with a whitespace added before and after.
 */
fun StringBuilder.appendWithSafeSpace(string: String): StringBuilder {
    append(" $string ")
    return this
}