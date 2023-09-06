package ch.heigvd

import ch.heigvd.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.security.MessageDigest
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    private fun ByteArray.toHexString() = joinToString (""){ "%02x".format(it) }

    @Test
    fun testhash() = testApplication {

        val pwd = "myprecious"
        val salt = "wfrf"

        val bytes = pwd.toByteArray() + salt.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")

        println( "====START====")
        val fr = md.digest(bytes)
        print(fr.toHexString())
        println( "====STOP====")
    }
}
