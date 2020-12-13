package jaaska.jaakko.photosapp.server

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import jaaska.jaakko.photosapp.server.extension.absoluteFilePath
import jaaska.jaakko.photosapp.server.extension.copyToSuspend
import jaaska.jaakko.photosapp.server.extension.noneAreNull
import jaaska.jaakko.photosapp.server.model.MediaMeta
import jaaska.jaakko.photosapp.server.model.MediaStatus
import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.io.File

@ObsoleteCoroutinesApi
val moduleProvider by lazy { ModuleProvider() }

@ObsoleteCoroutinesApi
fun main(args: Array<String>) {
    Logger.debugLogging = true

    val config = if (args.size < 2) {
        Logger.e("No configuration file defined!")
        null
    } else {
        moduleProvider.configLoader.loadConfig(args[1])
    }

    config?.also {
        val server = embeddedServer(
            Netty,
            port = it.serverPort,
            module = Application::module
            //watchPaths = listOf("photobackup-backend")
        )
        server.start(wait = true)
    } ?: run {
        Logger.e("No valid configuration!")
        return
    }
}

@ObsoleteCoroutinesApi
@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    val mediaRepository = moduleProvider.mediaRepository

    install(Authentication) {
        basic("defaultAdminAuth") {
            realm = "Ktor Server"
            validate { if (it.name == "jaakkoadmin" && it.password == "SalainenSana1324!@") UserIdPrincipal(it.name) else null }
        }

        basic("usersAuth") {
            // TODO Validate users
            realm = "Ktor Server"
            validate { if (it.name == "jaakkoadmin" && it.password == "SalainenSana1324!@") UserIdPrincipal(it.name) else null }
        }
    }

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("photosapp-backend", contentType = ContentType.Text.Plain)
        }

        //
        //
        // ADMIN ENDPOINTS
        authenticate("defaultAdminAuth") {
            post("/user") {
                // TODO Create new user
            }

            // Initiates a new library scan / processing process.
            get("/scanlibrary") {
                if (mediaRepository.scanLibrary()) {
                    // Library scan was started
                    call.respond(HttpStatusCode.OK)
                } else {
                    // A scan was already running
                    call.respond(HttpStatusCode.Conflict, "Scan is already running")
                }
            }

            // Returns status of current library scan / processing.
            //
            // If there's a scan in progress, or a scan has happened during this app lifecycle, the server will
            // respond with 200 and the status.
            //
            // If there has not been a scan during this app lifecycle, server responds with 409 and empty body.
            get("/scanstatus") {
                mediaRepository.libraryScanStatus?.also { status ->
                    call.respond(status)
                } ?: run {
                    call.respond(HttpStatusCode.Conflict)
                }
            }
        }

        //
        //
        // USER ENDPOINTS
        authenticate("usersAuth") {
            get("/protected/route/basic") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }

            route("/media") {
                get("/") {
                    val offset = call.request.queryParameters["offset"]?.toIntOrNull()
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()

                    val mediaMetas = if (noneAreNull(offset, limit)) {
                        mediaRepository.getAllMediaMeta(limit!!, offset!!)
                    } else {
                        mediaRepository.getAllMediaMeta()
                    }

                    call.respond(mediaMetas)
                }

                post("/") {
                    val receivedMeta = call.receive<MediaMeta>()
                    val responseMeta = mediaRepository.onMediaMetaReceived(receivedMeta)
                    val statusCode = if (responseMeta.first) HttpStatusCode.Created else HttpStatusCode.OK
                    call.respond(statusCode, responseMeta.second)
                }
            }

            route("/media/{id}") {
                get("/") {
                    call.parameters["id"]?.toInt()?.also { mediaId ->
                        mediaRepository.getMediaForId(mediaId)?.also {
                            call.respond(it)
                        } ?: run {
                            call.respond(HttpStatusCode.NotFound, "")
                        }
                    } ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                get("/thumbnail") {
                    call.parameters["id"]?.toInt()?.let { mediaId ->
                        mediaRepository.getMediaForId(mediaId)?.let { mediaMeta ->
                            val thumbPath = mediaRepository.getThumbnailPath(mediaMeta)
                            val thumbnailFile = File(thumbPath)
                            call.respondFile(thumbnailFile)
                        } ?: run {
                            call.respond(HttpStatusCode.NotFound, "")
                        }
                    } ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                get("/file") {
                    call.parameters["id"]?.toInt()?.let { mediaId ->
                        mediaRepository.getMediaForId(mediaId)?.let { mediaMeta ->
                            val mediaFile = File(mediaMeta.absoluteFilePath)
                            call.respondFile(mediaFile)
                        } ?: run {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                    }

                }
                post("/file") {
                    call.parameters["id"]?.toInt()?.let { mediaId ->
                        mediaRepository.getMediaForId(mediaId)?.let { mediaMeta ->
                            // Metadata for the media must exist, and it must be in "upload_pending" state
                            if (mediaMeta.status != MediaStatus.UPLOAD_PENDING) {
                                call.respond(HttpStatusCode.Conflict, "File already exists")
                            } else {
                                // Read the file, that should be there as multipart form data
                                val file = File(mediaMeta.dirPath, mediaMeta.fileName)
                                call.receiveMultipart().forEachPart { part ->
                                    if (part is PartData.FileItem) {
                                        part.originalFileName?.also { filename ->
                                            part.streamProvider().use { input ->
                                                file.outputStream().buffered()
                                                    .use { output -> input.copyToSuspend(output) }
                                            }
                                        }
                                    }
                                    part.dispose()
                                }

                                // Verify that the file matches the meta data. This will ensure that the file was
                                // received intact, and that the file was correct.
                                if (mediaRepository.mediaMatchesMeta(mediaMeta, file)) {
                                    call.respond(HttpStatusCode.Created)
                                    mediaRepository.onMediaFileReceived(mediaMeta)
                                } else {
                                    call.respond(HttpStatusCode.BadRequest, "File doesn't match metadata")
                                    file.delete()
                                }
                            }
                        } ?: run {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } ?: run {
                        call.respond(HttpStatusCode.BadRequest, "Missing media ID")
                    }
                }
                get("/exif") {
                    val mediaId = call.parameters["id"]
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }
    }
}

