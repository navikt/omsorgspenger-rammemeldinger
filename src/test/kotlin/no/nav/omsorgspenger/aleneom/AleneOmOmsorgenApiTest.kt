package no.nav.omsorgspenger.aleneom

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.omsorgspengerRammemeldinger
import no.nav.omsorgspenger.testutils.AuthorizationHeaders
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.WireMockExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class, WireMockExtension::class)
internal class AleneOmOmsorgenApiTest(
    dataSource: DataSource,
    wireMockServer: WireMockServer) {

    private val aleneOmOmsorgenServiceMock = mockk<AleneOmOmsorgenService>().also {
        every { it.hentSpleisetAleneOmOmsorgen(any(), any(), any()) }.returns(
            listOf(
                SpleisetAleneOmOmsorgen(
                    gjennomført = LocalDate.parse("2020-01-01"),
                    gyldigFraOgMed = LocalDate.parse("2020-02-01"),
                    gyldigTilOgMed = LocalDate.parse("2020-03-01"),
                    kilder = setOf(Kilde(id = "noe/fra/it", type = "Personkort")),
                    barn = Barn(id = "2020-01-01", type = "Fødselsdato", fødselsdato = LocalDate.parse("2010-01-01"))
                ),
                SpleisetAleneOmOmsorgen(
                    gjennomført = LocalDate.parse("2020-02-02"),
                    gyldigFraOgMed = LocalDate.parse("2020-03-01"),
                    gyldigTilOgMed = LocalDate.parse("2020-04-01"),
                    kilder = setOf(Kilde(id = "1234", type = "OmsorgspengerRammemeldinger")),
                    barn = Barn(id = "02022011111", type = "Identetsnummer", fødselsdato = LocalDate.parse("2020-02-02"))
                )
            )
        )
    }

    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate(),
        wireMockServer = wireMockServer
    ).also { builder ->
        builder.aleneOmOmsorgenService = aleneOmOmsorgenServiceMock
    }.build()

    @Test
    fun `hent alene om omsorgen`() {
        withTestApplication({ omsorgspengerRammemeldinger(applicationContext) }) {
            handleRequest(HttpMethod.Post, Path) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, AuthorizationHeaders.k9AarskvantumAuthorized())
                setBody(Body)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())

                @Language("JSON")
                val forventetResponse = """
                {
                    "aleneOmOmsorgen": [{
                        "gjennomført": "2020-01-01",
                        "gyldigFraOgMed": "2020-02-01",
                        "gyldigTilOgMed": "2020-03-01",
                        "barn": {
                            "id": "2020-01-01",
                            "type": "Fødselsdato",
                            "fødselsdato": "2010-01-01"
                        },
                        "kilder": [{
                            "id": "noe/fra/it",
                            "type": "Personkort"
                        }]
                    }, {
                        "gjennomført": "2020-02-02",
                        "gyldigFraOgMed": "2020-03-01",
                        "gyldigTilOgMed": "2020-04-01",
                        "barn": {
                            "id": "02022011111",
                            "type": "Identetsnummer",
                            "fødselsdato": "2020-02-02"
                        },
                        "kilder": [{
                            "id": "1234",
                            "type": "OmsorgspengerRammemeldinger"
                        }]
                    }]
                }
                """.trimIndent()

                JSONAssert.assertEquals(forventetResponse, response.content, true)
            }
        }
    }

    @Test
    fun `hent alene om omsorgen uten tilgang`() {
        withTestApplication({
            omsorgspengerRammemeldinger(applicationContext)
        }) {
            handleRequest(HttpMethod.Post, Path) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Body)
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }

            handleRequest(HttpMethod.Post, Path) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, AuthorizationHeaders.k9AarskvantumUnauthorized())
                setBody(Body)
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    companion object {
        private const val Path = "/hentAleneOmOmsorgen"
        private val Body = """
            {
                "identitetsnummer": "12345678900",
                "fom": "2020-01-01",
                "tom": "2020-12-31"
            }
        """.trimIndent()
    }
}