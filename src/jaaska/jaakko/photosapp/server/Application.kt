package jaaska.jaakko.photosapp.server

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import jaaska.jaakko.photosapp.server.model.MediaMeta

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
                moduleProvider.mediaRepository.rescanLibrary()
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
                    call.respondText("{\"files\": []}", contentType = ContentType.Application.Json)
                }

                post("/") {
                    call.respondText("POST to /media")
                }
            }

            route("/media/{id}") {
                get("/") {
                    val mediaId = call.parameters["id"]

                    val someMeta = MediaMeta(4, "namegoeswhere", 72, "dirpathhhhh", "hashhhh", "asdfadatetimeoriginal", "statusss")
                    call.respond(someMeta)
                    //call.respondText("GET to /media/$mediaId")
                }
                get("/thumbnail") {
                    val mediaId = call.parameters["id"]
                    call.respondText("GET to /media/$mediaId/thumbnail")
                }
                get("/file") {
                    val mediaId = call.parameters["id"]
                    call.respondText("GET to /media/$mediaId/file")
                }
                get("/exif") {
                    val mediaId = call.parameters["id"]
                    call.respondText("GET to /media/$mediaId/exif")
                }
            }
        }
    }
}

