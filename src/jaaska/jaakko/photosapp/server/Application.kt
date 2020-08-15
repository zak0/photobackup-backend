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
import jaaska.jaakko.photosapp.server.model.MediaMeta
import java.io.File

fun main(args: Array<String>) {
    val server = embeddedServer(
        Netty,
        port = 3000,
        module = Application::module,
        watchPaths = listOf("photobackup-backend")
    )
    server.start(wait = true)
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    Logger.debugLogging = true

    val moduleProvider = ModuleProvider()
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

            get("/rescan") {
                mediaRepository.rescanLibrary()
                call.respond(HttpStatusCode.OK)
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
                    val mediaMetas = mediaRepository.getAllMediaMeta()
                    call.respond(mediaMetas)
                }

                post("/") {
                    // TODO Check if a matching metadata (checksum and filesize) already exists in the database
                    //  If not, create new entry for given meta data into database
                    val mediaMeta = call.receive<MediaMeta>()
                    mediaRepository.onMediaMetaReceived(mediaMeta)
                    call.respond(HttpStatusCode.Created, mediaMeta)
                }
            }

            route("/media/{id}") {
                get("/") {
                    val mediaId = call.parameters["id"]?.toInt()?.also { mediaId ->
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
                    }?: run {
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
                            if (mediaMeta.status != "upload_pending") {
                                call.respond(HttpStatusCode.Conflict)
                            } else {
                                // Read the file, that should be there as multipart form data
                                call.receiveMultipart().forEachPart { part ->
                                    if (part is PartData.FileItem) {
                                        part.originalFileName?.also { filename ->
                                            val file = File(mediaMeta.dirPath, filename)
                                            part.streamProvider().use { input -> file.outputStream().buffered().use { output -> input.copyToSuspend(output) } }
                                        }
                                    }
                                    part.dispose()
                                }

                                // TODO Verify that the file matches the meta data. This will ensure that the file was
                                //  received intact, and that the file was correct.
                                call.respond(HttpStatusCode.Created)
                            }
                        } ?: run {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } ?: run {
                        call.respond(HttpStatusCode.BadRequest)
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

