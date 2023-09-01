package ch.heigvd.database

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.ResultSet

@Serializable
data class Ingredient(val id: Int, val name: String, val fiber: Double, val protein: Double, val energy: Int, val carb: Double, val fat: Double)

class IngredientService(private val connection: Connection) {
    companion object {
        private const val SELECT_ALL_INGREDIENTS = "SELECT * FROM grocerypal.ingredient;"
        private const val SELECT_INGREDIENT = "SELECT * FROM grocerypal.ingredient WHERE id = ?"
        private const val SELECT_IN_RECIPE = "SELECT * FROM grocerypal.ingredient " +
                "         INNER JOIN grocerypal.in_recipe_list irl on ingredient.id = irl.ingredient_id " +
                "WHERE recipe_id = ?"

    }

    // resultSet must not be empty!
    private fun resultSetToIngredient(resultSet: ResultSet) : Ingredient{
        val id      = resultSet.getInt("id")
        val name    = resultSet.getString("name")
        val fiber   = resultSet.getDouble("fiber")
        val protein = resultSet.getDouble("protein")
        val energy  = resultSet.getInt("energy")
        val carb    = resultSet.getDouble("carb")
        val fat     = resultSet.getDouble("fat")
        return Ingredient(id, name, fiber, protein, energy, carb, fat)
    }

    // Read an Ingredient
    suspend fun read(id: Int): Ingredient = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_INGREDIENT)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            return@withContext resultSetToIngredient(resultSet)
        } else {
            throw Exception("Record not found")
        }
    }

    suspend fun readAll() : ArrayList<Ingredient> = withContext(Dispatchers.IO)  {
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(SELECT_ALL_INGREDIENTS)

        val list : ArrayList<Ingredient> = ArrayList()

        while (resultSet.next()) {
            list.add(resultSetToIngredient(resultSet))
        }
        return@withContext list
    }

    // Read all ingredients listed in a recipe, with their quantity
    suspend fun readFromRecipe(recipeId: Int) : ArrayList<Ingredient> = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_IN_RECIPE)
        statement.setInt(1, recipeId)
        val resultSet = statement.executeQuery()

        val list : ArrayList<Ingredient> = ArrayList()

        while (resultSet.next()) {
            list.add(resultSetToIngredient(resultSet))
        }
        return@withContext list
    }
}
