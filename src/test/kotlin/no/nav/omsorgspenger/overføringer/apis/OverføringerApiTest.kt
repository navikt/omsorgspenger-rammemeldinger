package no.nav.omsorgspenger.overføringer.apis

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import java.time.ZonedDateTime
import javax.sql.DataSource
import kotlin.test.assertEquals

@ExtendWith(DataSourceExtension::class, WireMockExtension::class)
internal class OverføringerApiTest(
    dataSource: DataSource,
    wireMockServer: WireMockServer) {

    private val saksnummerServiceMock = mockk<SaksnummerService>().also {
        every { it.hentSaksnummerIdentitetsnummerMapping(any()) }.returns(mapOf(
            "101112" to "22",
            "131415" to "33"
        ))
    }

    private val overføringRepositoryMock = mockk<OverføringRepository>().also {
        every { it.hentAktiveOverføringer(setOf(SaksnummerHarIkkeOverføringer)) }.returns(emptyMap())
        every { it.hentAktiveOverføringer(setOf(SaksnummerHarOverføringer)) }.returns(
            mapOf(SaksnummerHarOverføringer to GjeldendeOverføringer(
                gitt = listOf(GjeldendeOverføringGitt(
                    gjennomført = ZonedDateTime.parse("2020-11-24T17:34:31.227Z"),
                    antallDager = 5,
                    status = GjeldendeOverføring.Status.Aktiv,
                    kilder = setOf(),
                    lovanvendelser = LovanvendelserTest.TestLovanvendelser,
                    periode = Periode("2020-01-01/2020-12-31"),
                    til = "101112"
                )),
                fått = listOf(GjeldendeOverføringFått(
                    gjennomført = ZonedDateTime.parse("2018-11-24T17:34:31.000Z"),
                    antallDager = 3,
                    status = GjeldendeOverføring.Status.Aktiv,
                    kilder = setOf(),
                    lovanvendelser = LovanvendelserTest.TestLovanvendelser,
                    periode = Periode("2019-01-01/2019-12-31"),
                    fra = "131415"
                ))
            )
        ))
    }

    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate(),
        wireMockServer = wireMockServer
    ).also { builder ->
        builder.overføringRepository = overføringRepositoryMock
        builder.saksnummerService = saksnummerServiceMock
    }.build()

    @Test
    fun `Hent overføringer for en person som har overføringer`() {
        withTestApplication({ omsorgspengerRammemeldinger(applicationContext) }) {
            handleRequest(HttpMethod.Get, path(SaksnummerHarOverføringer)) {
                addHeader(HttpHeaders.Authorization, AuthorizationHeaders.authorizedUser())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())

                @Language("JSON")
                val forventetResponse = """
                {
                  "gitt": [{
                    "gjennomført": "2020-11-24T17:34:31.227Z",
                    "til": {"saksnummer": "101112", "identitetsnummer": "22"},
                    "lovanvendelser": {
                      "2019-01-01/2019-03-05": {"Min lov (versjon 1) § 1-2 Noe kult, første ledd, andre punktum": [
                        "En to tre § lov",
                        "Samme periode."
                      ]},
                      "2021-12-30/2022-02-02": {"Min andre lov (versjon 2) § 1-4 Noe kult, første ledd, fjerde punktum": ["Det var som bare.."]},
                      "2020-02-03/2020-05-07": {"Min lov (versjon 1) § 1-3 Noe kult, første ledd, tredje punktum": ["By design"]}
                    },
                    "antallDager": 5,
                    "periode": "2020-01-01/2020-12-31",
                    "status": "Aktiv"
                  }],
                  "fått": [{
                    "gjennomført": "2018-11-24T17:34:31.000Z",
                    "fra": {"saksnummer": "131415", "identitetsnummer": "33"},
                    "lovanvendelser": {
                      "2019-01-01/2019-03-05": {"Min lov (versjon 1) § 1-2 Noe kult, første ledd, andre punktum": [
                        "En to tre § lov",
                        "Samme periode."
                      ]},
                      "2021-12-30/2022-02-02": {"Min andre lov (versjon 2) § 1-4 Noe kult, første ledd, fjerde punktum": ["Det var som bare.."]},
                      "2020-02-03/2020-05-07": {"Min lov (versjon 1) § 1-3 Noe kult, første ledd, tredje punktum": ["By design"]}
                    },
                    "antallDager": 3,
                    "periode": "2019-01-01/2019-12-31",
                    "status": "Aktiv"
                  }]
                }
                """.trimIndent()

                JSONAssert.assertEquals(forventetResponse, response.content, true)
            }
        }
    }

    @Test
    fun `Hent overføringer for en person som ikke har overføringer`() {
        withTestApplication({ omsorgspengerRammemeldinger(applicationContext) }) {
            handleRequest(HttpMethod.Get, path(SaksnummerHarIkkeOverføringer)) {
                addHeader(HttpHeaders.Authorization, AuthorizationHeaders.authorizedUser())
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
    fun `Hent annet enn aktive overføringer`() {
        withTestApplication({ omsorgspengerRammemeldinger(applicationContext) }) {
            handleRequest(HttpMethod.Get, path(SaksnummerHarOverføringer, "Deaktivert")) {
                addHeader(HttpHeaders.Authorization, AuthorizationHeaders.authorizedUser())
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    private companion object {
        private const val Path = "/overforinger"
        private const val SaksnummerHarOverføringer = "123"
        private const val SaksnummerHarIkkeOverføringer = "456"

        private fun path(saksnummer: Saksnummer, status: String = "Aktiv") =
            "$Path?saksnummer=$saksnummer&status=$status"
    }
}