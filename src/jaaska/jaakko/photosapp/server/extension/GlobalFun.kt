package jaaska.jaakko.photosapp.server.extension

val IS_OS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win")
val OS_PATH_SEPARATOR: String
    get() = if (IS_OS_WINDOWS) {
        "\\"
    } else {
        "/"
    }