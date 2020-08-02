package jaaska.jaakko.photosapp.server.extension

import jaaska.jaakko.photosapp.server.model.MediaMeta

val MediaMeta.absoluteFilePath: String
    get() = "${dirPath}${OS_PATH_SEPARATOR}${fileName}"
