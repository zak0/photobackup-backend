package jaaska.jaakko.photosapp.server.extension

val IS_OS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win")
val OS_PATH_SEPARATOR: String
    get() = if (IS_OS_WINDOWS) {
        "\\"
    } else {
        "/"
    }

fun <T, U, R> onNoneNull(p1: T?, p2: U?, block: (T, U) -> R?): R? = p1?.let { p2?.let { block(p1, p2) } }
