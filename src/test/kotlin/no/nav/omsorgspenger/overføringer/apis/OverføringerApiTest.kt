package no.nav.omsorgspenger.overføringer.apis

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime
import javax.sql.DataSource
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.lovverk.LovanvendelserTest
import no.nav.omsorgspenger.omsorgspengerRammemeldinger
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import no.nav.omsorgspenger.saksnummer.SaksnummerService
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

@ExtendWith(DataSourceExtension::class, WireMockExtension::class)
internal class OverføringerApiTest(
    dataSource: DataSource,
    wireMockServer: WireMockServer
) {

    private val tilgangsstyringMock = mockk<TilgangsstyringRestClient>().also {
        coEvery { it.sjekkTilgang(setOf("22", "33", "44", "55"), any(), any(), any()) }
            .returns(true)
    }

    private val saksnummerServiceMock = mockk<SaksnummerService>().also {
        every { it.hentSaksnummerIdentitetsnummerMapping(any()) }.returns(
            mapOf(
                "101112" to "22",
                "131415" to "33",
                SaksnummerHarOverføringer to "44",
                SaksnummerHarIkkeOverføringer to "55"
            )
        )
    }

    private val overføringRepositoryMock = mockk<OverføringRepository>().also {
        every { it.hentAktiveOverføringer(setOf(SaksnummerHarIkkeOverføringer)) }.returns(emptyMap())
        every { it.hentAktiveOverføringer(setOf(SaksnummerIkkeILøsningen)) }.returns(emptyMap())
        every { it.hentAktiveOverføringer(setOf(SaksnummerHarOverføringer)) }.returns(
            mapOf(
                SaksnummerHarOverføringer to GjeldendeOverføringer(
                    gitt = listOf(
                        GjeldendeOverføringGitt(
                            gjennomført = ZonedDateTime.parse("2020-11-24T17:34:31.227Z"),
                            antallDager = 5,
                            status = GjeldendeOverføring.Status.Aktiv,
                            kilder = setOf(),
                            lovanvendelser = LovanvendelserTest.TestLovanvendelser,
                            periode = Periode("2020-01-01/2020-12-31"),
                            til = "101112",
                            antallDagerØnsketOverført = 10
                        )
                    ),
                    fått = listOf(
                        GjeldendeOverføringFått(
                            gjennomført = ZonedDateTime.parse("2018-11-24T17:34:31.000Z"),
                            antallDager = 3,
                            status = GjeldendeOverføring.Status.Aktiv,
                            kilder = setOf(),
                            lovanvendelser = LovanvendelserTest.TestLovanvendelser,
                            periode = Periode("2019-01-01/2019-12-31"),
                            fra = "131415"
                        )
                    )
                )
            )
        )
    }

    private val testApplicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate(),
        wireMockServer = wireMockServer
    ).also { builder ->
        builder.overføringRepository = overføringRepositoryMock
        builder.saksnummerService = saksnummerServiceMock
        builder.tilgangsstyringRestClient = tilgangsstyringMock
    }.build()

    @Test
    fun `Hent overføringer for en person som har overføringer`() = testApplication {
        application {
            omsorgspengerRammemeldinger(testApplicationContext)
        }

        client.get(path(SaksnummerHarOverføringer)) {
            header(HttpHeaders.Authorization, AuthorizationHeaders.authorizedUser())
        }.apply {
            assertEquals(HttpStatusCode.OK, this.status)
            assertEquals(ContentType.Application.Json, this.contentType())

            @Language("JSON")
            val forventetResponse = """
                {
                    "saksnummer": "$SaksnummerHarOverføringer",
                    "identitetsnummer": "44",
                    "gitt": [{
                        "gjennomført": "2020-11-24T17:34:31.227Z",
                        "til": {
                            "saksnummer": "101112",
                            "identitetsnummer": "22"
                        },
                        "gjelderFraOgMed": "2020-01-01",
                        "begrunnelserForPeriode": [{
                                "gjelderFraOgMed": "2019-01-01",
                                "gjelderTilOgMed": "2019-03-05",
                                "begrunnelser": [
                                    "En to tre § lov",
                                    "Samme periode."
                                ]
                            },
                            {
                                "gjelderFraOgMed": "2020-02-03",
                                "gjelderTilOgMed": "2020-05-07",
                                "begrunnelser": ["By design"]
                            },
                            {
                                "gjelderFraOgMed": "2021-12-30",
                                "gjelderTilOgMed": "2022-02-02",
                                "begrunnelser": ["Det var som bare.."]
                            }
                        ],
                        "gjelderTilOgMed": "2020-12-31",
                        "dagerOverført": 5,
                        "dagerØnsketOverført": 10,
                        "status": "Aktiv"
                    }],
                    "fått": [{
                        "gjennomført": "2018-11-24T17:34:31.000Z",
                        "fra": {
                            "saksnummer": "131415",
                            "identitetsnummer": "33"
                        },
                        "gjelderFraOgMed": "2019-01-01",
                        "gjelderTilOgMed": "2019-12-31",
                        "dagerOverført": 3,
                        "status": "Aktiv"
                    }]
                }
                """.trimIndent()

            JSONAssert.assertEquals(forventetResponse, this.bodyAsText(), true)
        }

    }

    @Test
    fun `Hent overføringer for en person som ikke har overføringer`() = testApplication {
        application {
            omsorgspengerRammemeldinger(testApplicationContext)
        }

        client.get(path(SaksnummerHarIkkeOverføringer)) {
            header(HttpHeaders.Authorization, AuthorizationHeaders.authorizedUser())
        }.apply {
            assertEquals(HttpStatusCode.OK, this.status)
            assertEquals(ContentType.Application.Json, this.contentType())

            @Language("JSON")
            val forventetResponse = """
                {
                  "saksnummer": "$SaksnummerHarIkkeOverføringer",
                  "identitetsnummer": "55",
                  "gitt": [],
                  "fått": []
                }
                """.trimIndent()

            JSONAssert.assertEquals(forventetResponse, this.bodyAsText(), true)
        }
    }

    @Test
    fun `Hent overføringer for et saksnummer som ikke finnes i løsningen`() = testApplication {
        application {
            omsorgspengerRammemeldinger(testApplicationContext)
        }

        client.get(path(SaksnummerIkkeILøsningen)) {
            header(HttpHeaders.Authorization, AuthorizationHeaders.authorizedUser())
        }.apply {
            assertEquals(HttpStatusCode.NotFound, this.status)
        }
    }

    @Test
    fun `Hent annet enn aktive overføringer`() = testApplication {
        application {
            omsorgspengerRammemeldinger(testApplicationContext)
        }

        client.get(path(SaksnummerHarOverføringer, "Deaktivert")) {
            header(HttpHeaders.Authorization, AuthorizationHeaders.authorizedUser())
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, this.status)
        }
    }

    @Test
    fun `Hent overføringer uten tilgang ger http forbidden`() = testApplication {
        application {
            omsorgspengerRammemeldinger(testApplicationContext)
        }

        client.get(path(SaksnummerHarOverføringer)) {
            header(HttpHeaders.Authorization, AuthorizationHeaders.k9AarskvantumUnauthorized())
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, this.status)
        }
    }

    private companion object {
        private const val Path = "/overforinger"
        private const val SaksnummerHarOverføringer = "123"
        private const val SaksnummerHarIkkeOverføringer = "456"
        private const val SaksnummerIkkeILøsningen = "404"

        private fun path(saksnummer: Saksnummer, status: String = "Aktiv") =
            "$Path?saksnummer=$saksnummer&status=$status"
    }
}