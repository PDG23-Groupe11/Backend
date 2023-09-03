package ch.heigvd.database

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

fun Application.configureDatabases() {
    val dbConnection: Connection = connectToPostgres(embedded = false)
    val ingredientService = IngredientService(dbConnection)
    val recipeService = RecipeService(dbConnection)
    routing {
        route("/ingredients") {
            // Read all ingredients
            get() {
                try {
                    val ingredients = ingredientService.readAll()

                    call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(ingredients).toString())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            route("{id}") {
                // Read Ingredient
                get() {
                    val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                    try {
                        val ingredient = ingredientService.read(id)
                        call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(ingredient).toString())
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
            // Read all Ingredients linked to a recipe, with their quantity
            get("/from_recipe/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                try {
                    val ingredients = ingredientService.readFromRecipe(id)
                    call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(ingredients).toString())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        route("/recipes") {
            // Read all generic recipes
            get() {
                try {
                    val recipes = recipeService.readAllGeneric()

                    call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(recipes).toString())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            // TODO Protect
            // TODO get automatically the user id
            route("/personal") {
                route("{userId}") {
                    // Read all personal recipes
                    get() {
                        val id = call.parameters["userId"]?.toInt() ?: throw IllegalArgumentException("Invalid user ID")
                        try {
                            val recipes = recipeService.readAllPersonal(id)
                            call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(recipes).toString())
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Makes a connection to a Postgres database.
 * @param embedded -- if [true] defaults to an embedded database for tests that runs locally in the same process.
 * In this case you don't have to provide any parameters in configuration file, and you don't have to run a process.
 *
 * @return [Connection] that represent connection to the database. Please, don't forget to close this connection when
 * your application shuts down by calling [Connection.close]
 * */
fun Application.connectToPostgres(embedded: Boolean): Connection {
    Class.forName("org.postgresql.Driver")
    if (embedded) {
        return DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
    } else {
        val url = environment.config.property("postgres.url").getString()
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()

        return DriverManager.getConnection(url, user, password)
    }
}
