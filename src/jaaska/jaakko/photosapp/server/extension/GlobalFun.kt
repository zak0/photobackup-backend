package jaaska.jaakko.photosapp.server.extension

val IS_OS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win")
val OS_PATH_SEPARATOR: String
    get() = if (IS_OS_WINDOWS) {
        "\\"
    } else {
        "/"
    }

fun noneAreNull(vararg objects: Any?): Boolean {
    objects.forEach { if (it == null) return false }
    return true
}