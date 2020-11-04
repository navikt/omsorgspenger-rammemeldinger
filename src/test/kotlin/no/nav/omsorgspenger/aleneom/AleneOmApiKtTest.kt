package no.nav.omsorgspenger.aleneom

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

@ExtendWith(DataSourceExtension::class)
internal class AleneOmApiKtTest(
    private val dataSource: DataSource) {
    @Test
    fun `hent alene om omsorgen`() {
        withTestApplication({
            omsorgspengerRammemeldinger(TestApplicationContextBuilder(dataSource.cleanAndMigrate()).build())
        }) {
            handleRequest(HttpMethod.Post, "/hentAleneOmOmsorgen") {
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
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())

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