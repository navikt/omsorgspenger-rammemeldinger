package no.nav.omsorgspenger.aleneom.apis

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgen
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenRepository
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
import java.time.ZonedDateTime
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class, WireMockExtension::class)
internal class AleneOmOmsorgenApiTest(
    dataSource: DataSource,
    wireMockServer: WireMockServer) {

    private val aleneOmOmsorgenRepositoryMock = mockk<AleneOmOmsorgenRepository>().also {
        val aleneOmOmsorgen = AleneOmOmsorgen(
            registrert = ZonedDateTime.parse(registrert),
            periode = Periode("2020-01-01/2025-05-05"),
            barn = AleneOmOmsorgen.Barn(
                identitetsnummer = "12345678991",
                fødselsdato = LocalDate.parse("2006-05-01")
            ),
            behovssekvensId = "foo",
            regstrertIForbindelseMed = "bar"
        )
        every { it.hent("SAK1") }.returns(setOf(
            aleneOmOmsorgen,
            aleneOmOmsorgen.copy(periode = Periode("2025-03-03/2030-12-31"), behovssekvensId = "foo2", regstrertIForbindelseMed = "bar2"),
        ))
        every { it.hent("SAK2") }.returns(emptySet())
    }

    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate(),
        wireMockServer = wireMockServer
    ).also { builder ->
        builder.aleneOmOmsorgenRepository = aleneOmOmsorgenRepositoryMock
    }.build()

    @Test
    fun `hent alene om omsorgen har to meldinger`() {
        withTestApplication({ omsorgspengerRammemeldinger(applicationContext) }) {
            handleRequest(HttpMethod.Get, "/alene-om-omsorgen?saksnummer=SAK1") {
                addHeader(HttpHeaders.Authorization, AuthorizationHeaders.omsorgsdagerAuthorized())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                @Language("JSON")
                val forventet = """
                    {
                      "aleneOmOmsorgen": [{
                        "registrert": "$registrert",
                        "gjelderFraOgMed": "2020-01-01",
                        "gjelderTilOgMed": "2025-05-05",
                        "barn": {
                          "identitetsnummer": "12345678991",
                          "fødselsdato": "2006-05-01"
                        },
                        "kilde": {
                          "id":"foo",
                          "type":"OmsorgspengerRammemeldinger[bar]"
                        }
                      },{
                        "registrert": "$registrert",
                        "gjelderFraOgMed": "2025-03-03",
                        "gjelderTilOgMed": "2030-12-31",
                        "barn": {
                          "identitetsnummer": "12345678991",
                          "fødselsdato": "2006-05-01"
                        },
                        "kilde": {
                          "id":"foo2",
                          "type":"OmsorgspengerRammemeldinger[bar2]"
                        }
                      }]
                    }
                """.trimIndent()

                println(response.content!!)
                JSONAssert.assertEquals(forventet, response.content!!, true)
            }
        }
    }

    @Test
    fun `hent alene om omsorgen har ingen meldinger`() {
        withTestApplication({ omsorgspengerRammemeldinger(applicationContext) }) {
            handleRequest(HttpMethod.Get, "/alene-om-omsorgen?saksnummer=SAK2") {
                addHeader(HttpHeaders.Authorization, AuthorizationHeaders.omsorgsdagerAuthorized())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                @Language("JSON")
                val forventet = """
                    {
                      "aleneOmOmsorgen": []
                    }
                """.trimIndent()

                println(response.content!!)
                JSONAssert.assertEquals(forventet, response.content!!, true)
            }
        }
    }

    private companion object {
        private const val registrert = "2020-11-24T17:34:31.227Z"
    }
}