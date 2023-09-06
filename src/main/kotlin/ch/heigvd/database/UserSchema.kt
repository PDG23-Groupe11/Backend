package ch.heigvd.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection

@Serializable

data class User(val id : Int, val firstname: String, val name: String, val nbPerHome: Int, val email: String)
class UserService(private val connection: Connection) {

    companion object{
        private const val SELECT_USER_INFO = "SELECT id, firstname, name, Nb_per_home, FROM groceryPal.Profile WHERE email =  ? ;"
        private const val CREATE_USER = "INSERT INTO groceryPal.Profile(firstname, Name, nb_per_home, email, pwdHash, salt) VALUES (?,?,?,?,?,?) RETURNING id"
        private const val SELECT_SALT_AND_HASH = "SELECT pwd, salt FROM groceryPal.Profile WHERE email = ?;"
    }

    /**
     * Function to log a user with email and password
     * return the user info
     */
    suspend fun loginUser(email: String, pwd: String) : User? = withContext(Dispatchers.IO){
        // Find salt
        val statement = connection.prepareStatement(SELECT_SALT_AND_HASH)
        statement.setString(1, email)
        val resultSet = statement.executeQuery()

        if (!resultSet.next()) {
            return@withContext null
        }

        //get salt
        val saltHex = resultSet.getString("salt")
        val dbPwd = resultSet.getString("pwdHash")

        val salt = saltHex.decodeHex()
        // hash the password and salt
        val hashCode = hashPwd(pwd, salt)


        // compare the hash with the database
        // if correct return the user info
        if(hashCode.toHexString() == dbPwd) {
            val statementInfo = connection.prepareStatement(SELECT_USER_INFO)
            statementInfo.setString(1, email)
            val resultSetInfo = statementInfo.executeQuery()

            if(!resultSetInfo.next()) return@withContext null

            return@withContext User(
                resultSetInfo.getInt("id"),
                resultSetInfo.getString("firstname"),
                resultSetInfo.getString("name"),
                resultSetInfo.getInt("nb_per_home"),
                resultSetInfo.getString("email")
            )
        }
        // if not null
        return@withContext null
    }

    /**
     * function to create a new user
     * id field isn't used
     */
    suspend fun  createUser(user: User, password: String) = withContext(Dispatchers.IO) {
        connection.createStatement().execute("BEGIN TRANSACTION ;")
        try {
            val statement = connection.prepareStatement(CREATE_USER)
            // firstname, Name, nb_per_home, email, pwdHash, salt
            statement.setString(1, user.firstname)
            statement.setString(2, user.name)
            statement.setInt(3, user.nbPerHome)
            statement.setString(4, user.email)

            val salt = generateRandomSalt()
            val hashHex = hashPwd(password, salt).toHexString()
            val saltHex = salt.toHexString()
            statement.setString(5, hashHex)
            statement.setString(6, saltHex)

            val resultSet = statement.executeQuery()
            if (!resultSet.next()){
                throw InternalError("Could not create user\n")
            }

            connection.createStatement().execute("COMMIT;")

            return@withContext resultSet.getInt("id")

        } catch (e : Exception) {
            connection.createStatement().execute("ROLLBACK;")
            throw e
        }
    }

    /**
     * Function : hash the password with a salt
     * return : a ByteArray of the hash
     * info found there :
     * https://gist.github.com/lovubuntu/164b6b9021f5ba54cefc67f60f7a1a25
     */
    private fun hashPwd(pwd: String, salt: ByteArray): ByteArray {
        val bytes = pwd.toByteArray() + salt
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes)
    }

    /**
     * Function to generate a salt
     * info found there :
     * https://codersee.com/kotlin-pbkdf2-secure-password-hashing/
     */
    private fun generateRandomSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    /**
     * Function to convert a byteArray to a hexadecimal string
     * info found there :
     * https://stackoverflow.com/questions/52225212/converting-a-byte-array-into-a-hex-string
     */
    private fun ByteArray.toHexString() = joinToString (""){ "%02x".format(it) }

    /**
     * Function to convert a hex String to a byteArray
     * info found there :
     * https://stackoverflow.com/questions/66613717/kotlin-convert-hex-string-to-bytearray
     */
    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}

