package ch.heigvd.database

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.ResultSet

@Serializable
data class Ingredient(val id: Int, val name: String, val fiber: Double, val protein: Double, val energy: Int, val carb: Double, val fat: Double)

class IngredientService(private val connection: Connection) {
    @Serializable
    data class Quantity(val ingredientId: Int, val unitId : Int, val quantity: Int)
    @Serializable
    data class InRecipe(val id: Int, val name: String, val fiber: Double, val protein: Double, val energy: Int, val carb: Double, val fat: Double, val unitId : Int, val quantity: Int)
    @Serializable
    data class InList(val id: Int, val unitId : Int, val quantity: Int, val buy : Boolean)
    companion object {
        private const val SELECT_ALL_INGREDIENTS = "SELECT * FROM grocerypal.ingredient;"
        private const val SELECT_INGREDIENT = "SELECT * FROM grocerypal.ingredient WHERE id = ?"
        private const val SELECT_IN_RECIPE = "SELECT id, name, fiber, protein, energy, carb, fat, unit_id, quantity\n" +
                "    FROM grocerypal.ingredient\n" +
                "        INNER JOIN grocerypal.in_recipe_list irl on ingredient.id = irl.ingredient_id\n" +
                "    WHERE recipe_id = ?"
        private const val INSERT_IN_RECIPE = "INSERT INTO grocerypal.in_recipe_list (recipe_id, ingredient_id, unit_id, quantity) VALUES (?,?,?,?)"
        private const val SELECT_IN_LIST = "SELECT id, unit_id, quantity, buy\n" +
                "FROM grocerypal.ingredient\n" +
                "         INNER JOIN grocerypal.in_shopping_list isl on ingredient.id = isl.ingredient_id\n" +
                "WHERE profile_id = ?\n"
        private const val DELETE_LIST = "DELETE FROM grocerypal.in_shopping_list WHERE profile_id = ?"
        private const val INSERT_IN_LIST = "INSERT INTO grocerypal.in_shopping_list (profile_id, ingredient_id, unit_id, quantity, buy) VALUES (?,?,?,?,?)"

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

    // resultSet must not be empty!
    private fun resultSetToInRecipe(resultSet: ResultSet) : InRecipe{
        val id      = resultSet.getInt("id")
        val name    = resultSet.getString("name")
        val fiber   = resultSet.getDouble("fiber")
        val protein = resultSet.getDouble("protein")
        val energy  = resultSet.getInt("energy")
        val carb    = resultSet.getDouble("carb")
        val fat     = resultSet.getDouble("fat")
        val unit_id = resultSet.getInt("unit_id")
        val quantity= resultSet.getInt("quantity")
        return InRecipe(id, name, fiber, protein, energy, carb, fat, unit_id, quantity)
    }
    // resultSet must not be empty!
    private fun resultSetToInList(resultSet: ResultSet) : InList{
        val id      = resultSet.getInt("id")
        val unit_id = resultSet.getInt("unit_id")
        val quantity= resultSet.getInt("quantity")
        val buy     = resultSet.getBoolean("buy")
        return InList(id, unit_id, quantity, buy)
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
    suspend fun readFromRecipe(recipeId: Int) : ArrayList<InRecipe> = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_IN_RECIPE)
        statement.setInt(1, recipeId)
        val resultSet = statement.executeQuery()

        val list : ArrayList<InRecipe> = ArrayList()

        while (resultSet.next()) {
            list.add(resultSetToInRecipe(resultSet))
        }
        return@withContext list
    }
    // Insert an ingredient in a recipe
    suspend fun insertInRecipe(recipeId:Int, quantity: Quantity) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(INSERT_IN_RECIPE)
        statement.setInt(1, recipeId)
        statement.setInt(2, quantity.ingredientId)
        statement.setInt(3, quantity.unitId)
        statement.setInt(4, quantity.quantity)

        // TODO check if succeeded
        statement.execute()
    }

    // Read all ingredients listed in a shopping list, with their quantity
    suspend fun readFromList(userId: Int) : ArrayList<InList> = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_IN_LIST)
        statement.setInt(1, userId)
        val resultSet = statement.executeQuery()

        val list : ArrayList<InList> = ArrayList()

        while (resultSet.next()) {
            list.add(resultSetToInList(resultSet))
        }
        return@withContext list
    }

    // Rewrite the content of a shopping list
    suspend fun setList(userId: Int, ingredients : List<InList>) = withContext(Dispatchers.IO) {
        connection.createStatement().execute("BEGIN TRANSACTION ;")
        try {
            // delete the existing list
            val delStatement = connection.prepareStatement(DELETE_LIST)
            delStatement.setInt(1, userId)
            delStatement.execute()

            // Add all the items
            for (ingredient in ingredients) {
                insertInList(userId, ingredient)
            }
            // if successful, commit the changes
            connection.createStatement().execute("COMMIT;")
        } catch (e : Exception) {
            connection.createStatement().execute("ROLLBACK;")
            throw e
        }
    }

    // Insert a single ingredient in a recipe
    suspend fun insertInList(userId:Int, ingredient: InList) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(INSERT_IN_LIST)
        statement.setInt(1, userId)
        statement.setInt(2, ingredient.id)
        statement.setInt(3, ingredient.unitId)
        statement.setInt(4, ingredient.quantity)
        statement.setBoolean(5, ingredient.buy)

        // TODO check if succeeded
        statement.execute()
    }

}
