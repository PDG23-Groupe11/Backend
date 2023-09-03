package ch.heigvd.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.ResultSet


@Serializable
data class Recipe(val id: Int, val name: String, val nb_per: Int, val prep_time: Int, val instruction: String)
class RecipeService(private val connection: Connection, private val ingredientService: IngredientService){
    @Serializable
    data class CompleteRecipe(val recipe: Recipe, val ingredients: List<IngredientService.Quantity>)
    companion object {
        private const val SELECT_ALL_RECIPES = "SELECT id, name, nb_per, prep_time, instruction FROM grocerypal.recipe WHERE profile_id IS NULL;"
        private const val SELECT_PERSONAL_RECIPE= "SELECT id, name, nb_per, prep_time, instruction FROM grocerypal.recipe WHERE profile_id = ?"
        private const val INSERT_PERSONAL_RECIPE = "INSERT INTO grocerypal.recipe(name, nb_per, prep_time, instruction, profile_id) VALUES (?,?,?,?,?) RETURNING id"


    }

    // resultSet must not be empty!
    private fun resultSetToRecipe(resultSet: ResultSet) : Recipe{
        val id           = resultSet.getInt("id")
        val name         = resultSet.getString("name")
        val nb_per       = resultSet.getInt("nb_per")
        val prep_time    = resultSet.getInt("prep_time")
        val instruction  = resultSet.getString("instruction")

        return Recipe(id, name, nb_per, prep_time, instruction)
    }

    // Get all the generic (not linked to a profile) recipes
    suspend fun readAllGeneric() : List<Recipe> = withContext(Dispatchers.IO)  {
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(SELECT_ALL_RECIPES)

        val list : ArrayList<Recipe> = ArrayList()

        while (resultSet.next()) {
            list.add(resultSetToRecipe(resultSet))
        }
        return@withContext list
    }

    // Get all the personal recipes
    suspend fun readAllPersonal(userId : Int) : List<Recipe> = withContext(Dispatchers.IO)  {
        val statement = connection.prepareStatement(SELECT_PERSONAL_RECIPE)
        statement.setInt(1, userId)
        val resultSet = statement.executeQuery()

        val list : ArrayList<Recipe> = ArrayList()

        while (resultSet.next()) {
            list.add(resultSetToRecipe(resultSet))
        }
        return@withContext list
    }

    // Create a new personal recipe
    // TODO : verify values
    suspend fun createPersonal(userId: Int, completeRecipe: CompleteRecipe) = withContext(Dispatchers.IO) {
        connection.createStatement().execute("BEGIN TRANSACTION ;")
        try {
            val statement = connection.prepareStatement(INSERT_PERSONAL_RECIPE)
            // name, nb_per, prep_time, instruction, profile_id
            statement.setString(1, completeRecipe.recipe.name)
            statement.setInt(2, completeRecipe.recipe.nb_per)
            statement.setInt(3, completeRecipe.recipe.prep_time)
            statement.setString(4, completeRecipe.recipe.instruction)
            statement.setInt(5, userId)

            val resultSet = statement.executeQuery()
            // TODO error handling
            if(resultSet.next()) {
                val recipeId = resultSet.getInt("id")
                // Add all the ingredients
                for (quantity in completeRecipe.ingredients) {
                    ingredientService.insertInRecipe(recipeId, quantity)
                }
                connection.createStatement().execute("COMMIT;")
                }
        } catch (e : Exception) {
            connection.createStatement().execute("ROLLBACK;")
            throw e
        }
    }
}