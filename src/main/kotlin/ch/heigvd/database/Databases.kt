package ch.heigvd.database

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
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
    val recipeService = RecipeService(dbConnection, ingredientService)
    val userService = UserService(dbConnection)
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
            // TODO protection
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
            route("/personal") {
                // Read all personal recipes
                get() {
                    val userId = handleToken(call.request.authorization(), userService)
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid authentification token")
                        return@get
                    }
                    try {
                        val recipes = recipeService.readAllPersonal(userId)
                        call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(recipes).toString())
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                // Create a new personal recipe
                post() {
                    val userId = handleToken(call.request.authorization(), userService)
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid authentification token")
                        return@post
                    }
                    try {
                        val recipeJson = call.receiveText()
                        val recipe = Json.decodeFromString<RecipeService.CompleteRecipe>(recipeJson)

                        recipeService.createPersonal(userId, recipe)
                        call.respond(HttpStatusCode.OK)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.NotAcceptable)
                    }
                }
            }
        }
        route("/list") {
            // Read the user's list
            get() {
                val userId = handleToken(call.request.authorization(), userService)
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid authentification token")
                    return@get
                }
                try {
                    val ingredients = ingredientService.readFromList(userId)
                    call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(ingredients).toString())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            // Create a new personal recipe
            post() {
                val userId = handleToken(call.request.authorization(), userService)
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid authentification token")
                    return@post
                }
                try {
                    val ingredientsJson = call.receiveText()
                    val ingredients = Json.decodeFromString<List<IngredientService.InList>>(ingredientsJson)

                    ingredientService.setList(userId, ingredients)
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.NotAcceptable)
                }
            }
        }
        route("/account"){
            post("/login"){
                try {
                    val credentialsJson = call.receiveText()
                    val credentialsDec = Json.decodeFromString<Credentials>(credentialsJson)

                    val token = userService.loginUser(credentialsDec)

                    if (!token.isNullOrEmpty()) {
                        // Authentication successful
                        val tokenJson = TokenStruct(token)
                        // send the token to the client
                        call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(tokenJson).toString())
                    } else {
                        //  Authentication failed
                        call.respond(HttpStatusCode.Unauthorized, "Incorrect credentials")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
                }
            }

            post("/createAccount"){
                try {
                    val userJson = call.receiveText()
                    val userDec = Json.decodeFromString<FullUser>(userJson)

                    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@(.+)$")
                    val isEmailValid = emailRegex.matches(userDec.email)

                    if (userDec.firstname.isBlank() || userDec.name.isBlank() || !isEmailValid){
                        call.respond(HttpStatusCode.BadRequest, "Incorrect field")
                        return@post
                    }

                    val response = userService.createUser(userDec)

                    if (response) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Not able to create user")
                    }
                } catch (e: Exception) {
                    println(e)
                    call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
                }
            }

        }
        route ("/user"){
            get(){
                try {
                    val authHeader = call.request.authorization()
                    if (authHeader == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Missing auth token")
                        return@get
                    }
                    val token = authHeader.split("Bearer ")[1]

                    val info = userService.getUserInfo(token)
                    if(info == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid auth token")
                    } else {
                        call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(info).toString())
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
                }
            }
            post(){
                try {
                    val userId = handleToken(call.request.authorization(), userService)
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid authentification token")
                        return@post
                    }
                    val user = Json.decodeFromString<User>(call.receiveText())
                    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@(.+)$")
                    val isEmailValid = emailRegex.matches(user.email)
                    if (user.firstname.isBlank() || user.name.isBlank() || !isEmailValid){
                        call.respond(HttpStatusCode.BadRequest, "Incorrect field")
                        return@post
                    }
                    userService.updateUser(userId, user)
                    call.respond(HttpStatusCode.OK)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
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

/**
 * Takes an authorization header value, and try to find the linked user. Null if not found
 */
suspend fun handleToken(authorizationHeader: String?, userService: UserService): Int? {
    if (authorizationHeader == null) {
        return null
    }
    val token = authorizationHeader.split("Bearer ")[1]
    return userService.getUserId(token)
}
