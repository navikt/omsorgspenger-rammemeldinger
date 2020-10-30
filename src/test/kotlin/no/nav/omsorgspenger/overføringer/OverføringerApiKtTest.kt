package no.nav.omsorgspenger.overføringer

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgspenger.omsorgspengerRammemeldinger
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertEquals

internal class OverføringerApiKtTest {
    @Test
    internal fun `hent overføringer`() {
        withTestApplication({
            omsorgspengerRammemeldinger(TestAppliationContextBuilder().build())
        }) {
            handleRequest(HttpMethod.Post, "/hentOverfoeringer") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""
                    {
                        "personIdent": "12345678900",
                        "fom": "2020-01-01",
                        "tom": "2020-12-31"
                    }
                """.trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())

                @Language("JSON")
                val forventetResponse = """
                    {
                      "gitt": [],
                      "fått": []
                    }
                """.trimIndent()

                JSONAssert.assertEquals(forventetResponse, response.content, true)
            }
        }
    }

    @Test
    internal fun `hent alene om omsorgen`() {
        withTestApplication({
            omsorgspengerRammemeldinger(TestAppliationContextBuilder().build())
        }) {
            handleRequest(HttpMethod.Post, "/hentAleneOmOmsorgen") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""
                    {
                        "personIdent": "12345678900",
                        "fom": "2020-01-01",
                        "tom": "2020-12-31"
                    }
                """.trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())

                @Language("JSON")
                val forventetResponse = """
                    {
                      "aleneOmOmsorgen": []
                    }
                """.trimIndent()

                JSONAssert.assertEquals(forventetResponse, response.content, true)
            }
        }
    }
}