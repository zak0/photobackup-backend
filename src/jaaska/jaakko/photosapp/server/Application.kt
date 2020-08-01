package jaaska.jaakko.photosapp.server

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
                    call.respondText("POST to /media")
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
                    val mediaId = call.parameters["id"]
                    call.respond(HttpStatusCode.NotImplemented)
                }
                get("/file") {
                    call.parameters["id"]?.toInt()?.let { mediaId ->
                        mediaRepository.getMediaForId(mediaId)?.let { mediaMeta ->
                            val mediaFile = File("${mediaMeta.dirPath}\\${mediaMeta.fileName}")
                            call.respondFile(mediaFile)
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

