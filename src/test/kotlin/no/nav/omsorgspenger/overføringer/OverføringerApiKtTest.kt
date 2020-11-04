package no.nav.omsorgspenger.overføringer

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgspenger.omsorgspengerRammemeldinger
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import javax.sql.DataSource
import kotlin.test.assertEquals

@ExtendWith(DataSourceExtension::class)
internal class OverføringerApiKtTest(
    private val dataSource: DataSource) {
    @Test
    fun `hent overføringer`() {
        withTestApplication({
            omsorgspengerRammemeldinger(TestApplicationContextBuilder(dataSource.cleanAndMigrate()).build())
        }) {
            handleRequest(HttpMethod.Post, "/hentOverfoeringer") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())

                @Language("JSON")
                val body = """
                    {
                        "identitetsnummer": "12345678900",
                        "fom": "2020-01-01",
                        "tom": "2020-12-31"
                    }
                """.trimIndent()
                setBody(body)
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
}