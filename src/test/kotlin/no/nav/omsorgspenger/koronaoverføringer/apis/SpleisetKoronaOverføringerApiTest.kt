package no.nav.omsorgspenger.koronaoverføringer.apis

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.omsorgspengerRammemeldinger
import no.nav.omsorgspenger.overføringer.apis.*
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
import java.time.Duration
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class, WireMockExtension::class)
internal class SpleisetKoronaOverføringerApiTest(
    dataSource: DataSource,
    wireMockServer: WireMockServer
) {

    private val spleisetoverføringerServiceMock = mockk<SpleisetKoronaOverføringerService>().also {
        coEvery { it.hentSpleisetOverføringer(any(), any(), any()) }.returns(
            SpleisetOverføringer(
                gitt = listOf(
                    SpleisetOverføringGitt(
                        gjennomført = LocalDate.parse("2018-01-01"),
                        gyldigFraOgMed = LocalDate.parse("2019-01-01"),
                        gyldigTilOgMed = LocalDate.parse("2020-01-01"),
                        til = Motpart(id = "29098811111", type = "Identitetsnummer"),
                        lengde = Duration.ofDays(5),
                        kilder = setOf(Kilde(id = "1", type = "OmsorgspengerRammemeldinger"))
                    )
                ),
                fått = listOf(
                    SpleisetOverføringFått(
                        gjennomført = LocalDate.parse("2019-01-01"),
                        gyldigFraOgMed = LocalDate.parse("2020-01-01"),
                        gyldigTilOgMed = LocalDate.parse("2021-01-01"),
                        fra = Motpart(id = "29098811111", type = "Identitetsnummer"),
                        lengde = Duration.ofDays(3),
                        kilder = setOf(Kilde(id = "2", type = "OmsorgspengerRammemeldinger"))

                    )
                )
            )
        )
    }

    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate(),
        wireMockServer = wireMockServer
    ).also { builder ->
        builder.spleisetKoronaOverføringerService = spleisetoverføringerServiceMock
    }.build()

    @Test
    fun `hent overføringer`() = testApplication {
        application {
            omsorgspengerRammemeldinger(applicationContext)
        }

        client.post(Path) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, AuthorizationHeaders.k9AarskvantumAuthorized())
            setBody(Body)
        }.apply {
            assertEquals(HttpStatusCode.OK, this.status)

            @Language("JSON")
            val forventetResponse = """
                {
                    "gitt": [{
                        "gjennomført": "2018-01-01",
                        "gyldigFraOgMed": "2019-01-01",
                        "gyldigTilOgMed": "2020-01-01",
                        "til": {
                            "id": "29098811111",
                            "type": "Identitetsnummer"
                        },
                        "lengde": "PT120H",
                        "kilder": [{
                            "id": "1",
                            "type": "OmsorgspengerRammemeldinger"
                        }]
                    }],
                    "fått": [{
                        "gjennomført": "2019-01-01",
                        "gyldigFraOgMed": "2020-01-01",
                        "gyldigTilOgMed": "2021-01-01",
                        "fra": {
                            "id": "29098811111",
                            "type": "Identitetsnummer"
                        },
                        "lengde": "PT72H",
                        "kilder": [{
                            "id": "2",
                            "type": "OmsorgspengerRammemeldinger"
                        }]
                    }]
                }
                """.trimIndent()

            JSONAssert.assertEquals(forventetResponse, this.bodyAsText(), true)
        }

    }

    @Test
    fun `hent overføringer uten tilgang`() = testApplication {
        application {
            omsorgspengerRammemeldinger(applicationContext)
        }

        client.post(Path) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(Body)
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, this.status)
        }

        client.post(Path) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, AuthorizationHeaders.k9AarskvantumUnauthorized())
            setBody(Body)
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, this.status)
        }
    }

    private companion object {
        private const val Path = "/hent-korona-overforinger"

        @Language("JSON")
        private val Body = """
            {
                "identitetsnummer": "12345678900",
                "fom": "2020-01-01",
                "tom": "2020-12-31"
            }
        """.trimIndent()
    }
}