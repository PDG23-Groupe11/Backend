package ch.heigvd.plugins

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting() {
    val recipeImagesPath = "static/recipeImages"
    val supportedExtensions = arrayOf("png", "jpeg", "jpg", "gif", "webp")

    // Create the folder for the pictures in case they don't exist
    File(recipeImagesPath).mkdirs()

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

        route("/static") {
            route("/recipeImages") {
                staticFiles("/", File(recipeImagesPath)) {
                        extensions(*supportedExtensions)
                }

                // Receive new/updated pictures
                // Code from https://ryanharrison.co.uk/2018/09/20/ktor-file-upload-download.html
                // TODO size limit
                // TODO file verification
                post("{recipeId}") {
                    try {
                        val recipeId =
                            call.parameters["recipeId"]?.toInt() ?: throw IllegalArgumentException("Invalid recipe ID")

                        // retrieve all multipart data (suspending)
                        val multipart = call.receiveMultipart()
                        multipart.forEachPart { part ->
                            // if part is a file (could be form item)
                            if (part is PartData.FileItem) {
                                // retrieve file name of upload
                                val extension = part.originalFileName!!.split(".").last()
                                if(!supportedExtensions.contains(extension)) {
                                    part.dispose()
                                    throw IllegalArgumentException("File type not supported")
                                }
                                val newFile = File("$recipeImagesPath/$recipeId.$extension")

                                // Delete the old file
                                // https://stackoverflow.com/a/61188020/7500975
                                val path = File(recipeImagesPath)
                                for (oldFile in path.walk().filter { it.nameWithoutExtension == recipeId.toString() }) {
                                    oldFile.delete()
                                }

                                // use InputStream from part to save file
                                part.streamProvider().use { its ->
                                    // copy the stream to the file with buffering
                                    newFile.outputStream().buffered().use {
                                        // note that this is blocking
                                        its.copyTo(it)
                                    }
                                }
                            }
                            // make sure to dispose of the part after use to prevent leaks
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK, "uploaded")

                    } catch (e : Exception) {
                        call.respond(HttpStatusCode.NotAcceptable, e.message ?: e.toString())
                    }
                }

            }
        }
    }
}
