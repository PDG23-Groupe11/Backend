package ch.heigvd

import ch.heigvd.database.configureDatabases
import ch.heigvd.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureSecurity()
    configureRouting()
    configureDatabases()
}
