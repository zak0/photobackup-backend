package jaaska.jaakko

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import jaaska.jaakko.photosapp.server.module

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }
}
