package ch.heigvd.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Service home page
        get("/") {
            call.respondText("Hello World!")
        }

        // Basic status report
        get("/status") {
            call.respond(HttpStatusCode.OK, "Still alive!\nhttps://www.youtube.com/watch?v=VuLktUzq23c")
        }
        swaggerUI(path = "openapi")
    }
}
