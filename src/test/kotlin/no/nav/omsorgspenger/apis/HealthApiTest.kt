package no.nav.omsorgspenger.apis

import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.server.testing.*
import no.nav.omsorgspenger.omsorgspengerRammemeldinger
import no.nav.omsorgspenger.overføringer.TestAppliationContextBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class HealthApiTest {

    @Test
    fun `Test health end point`() {
        withTestApplication({
            omsorgspengerRammemeldinger(TestAppliationContextBuilder().build())
        }) {
            handleRequest(Get, "/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
            }
        }
    }
}