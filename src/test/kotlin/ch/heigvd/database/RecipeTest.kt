package ch.heigvd.database

import ch.heigvd.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.test.*

class RecipeTest {

    @Test
    fun getAllGenericRecipes() = testApplication {
        environment {
            config = ApplicationConfig("application-test.conf")
        }
        application {
            configureDatabases()
        }

        // Given a recipe existing in the database
        val recipe = Recipe(9999998, "TEST_RECIPE", 4, 90, "TEST_INSTRUCTION\r\n**very testy / tasty**")

        // When we try to get it
        client.get("/recipes").apply {
            // Then the request is successful, and the recipe is present
            assertEquals(HttpStatusCode.OK, status)
            val returnedValue = Json.decodeFromString<List<Recipe>>(bodyAsText())
            assertContains(returnedValue, recipe)
        }
    }

    @Test
    fun getAllPersonalRecipes() = testApplication {
        environment {
            config = ApplicationConfig("application-test.conf")
        }
        application {
            configureDatabases()
        }

        // Given a personal recipe existing in the database
        val recipe = Recipe(9999999, "TEST_RECIPE_PERSO", 1, 10, "TEST_INSTRUCTION_PERSO\r\n**very testy / tasty**")

        // When we try to get it
        client.get("/recipes/personal") {
            bearerAuth("12f08b1f-9c6d-4208-8a47-50b64dab5f5e")
        }.apply {

            // Then the request is successful, and the recipe is present
            assertEquals(HttpStatusCode.OK, status)
            val returnedValue = Json.decodeFromString<List<Recipe>>(bodyAsText())
            assertEquals(recipe, returnedValue[0])
        }
    }
}
