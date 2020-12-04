package no.nav.omsorgspenger.infotrygd

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OmsorgspengerInfotrygdRammevedtakGatewayTest {

    @Test
    fun `Hente rammevedtak fra infotrygd`() {
        val identitetsnummer = "11111111111"
        val periode = Periode("2020-01-01/2020-12-31")

        val forventedeInfotrygdRammer = listOf(
            InfotrygdUtvidetRettVedtak(
                periode = Periode("2020-06-21/2020-06-21"),
                barn = InfotrygdAnnenPart(
                    id = "01019911111",
                    type = "Identitetsnummer",
                    fødselsdato = LocalDate.parse("1999-01-01")
                ),
                kilder = setOf(Kilde(
                    id = "UTV.RETT/20D/01019911111",
                    type = "Personkort"
                )),
                vedtatt = LocalDate.parse("2020-06-21")
            ),
            InfotrygdUtvidetRettVedtak(
                periode = Periode("2020-01-01/2025-12-31"),
                barn = InfotrygdAnnenPart(
                    id = "2001-01-01",
                    type = "Fødselsdato",
                    fødselsdato = LocalDate.parse("2001-01-01")
                ),
                vedtatt = LocalDate.parse("2020-06-11"),
                kilder = setOf(
                    Kilde(
                        id = "UTV.RETT/10D/01010111111",
                        type = "Personkort"
                    ),
                    Kilde(
                        id = "467035394",
                        type = "Brev"
                    )
                )
            ),
            InfotrygdFordelingGirMelding(
                periode = Periode("2017-06-17/2018-06-20"),
                vedtatt = LocalDate.parse("2018-06-17"),
                kilder = setOf(Kilde(
                    id = "ford/gir",
                    type = "Personkort"
                )),
                lengde = Duration.parse("P1DT5H45M")
            ),
            InfotrygdMidlertidigAleneVedtak(
                periode = Periode("1998-06-25/2001-06-25"),
                vedtatt = LocalDate.parse("1998-06-21"),
                kilder = setOf(Kilde(
                    id = "midl.alene.om/17D",
                    type = "Personkort"
                ))
            ),
            InfotrygdAleneOmOmsorgenMelding(
                periode = Periode("2017-06-20/2018-06-25"),
                vedtatt = LocalDate.parse("2016-06-18"),
                kilder = setOf(),
                barn = InfotrygdAnnenPart(
                    id = "1991-09-29",
                    fødselsdato = LocalDate.parse("1991-09-29"),
                    type = "Fødselsdato"
                )
            ),
            InfotrygdAleneOmOmsorgenMelding(
                periode = Periode("2017-06-17/2018-06-20"),
                vedtatt = LocalDate.parse("2016-06-17"),
                kilder = setOf(),
                barn = InfotrygdAnnenPart(
                    id = "29099011112",
                    fødselsdato = LocalDate.parse("1990-09-29"),
                    type = "Identitetsnummer"
                )
            ),
            InfotrygdOverføringGirMelding(
                vedtatt = LocalDate.parse("2018-06-17"),
                periode = Periode("2017-06-17/2018-06-20"),
                kilder = setOf(),
                lengde = Duration.parse("P1DT12H"),
                til = InfotrygdAnnenPart(
                    id = "29099011111",
                    type = "Identitetsnummer",
                    fødselsdato = LocalDate.parse("1990-09-29")
                )
            ),
            InfotrygdOverføringFårMelding(
                vedtatt = LocalDate.parse("2018-06-17"),
                periode = Periode("2017-06-17/2018-06-20"),
                kilder = setOf(),
                lengde = Duration.parse("P1D"),
                fra = InfotrygdAnnenPart(
                    id = "1990-09-29",
                    type = "Fødselsdato",
                    fødselsdato = LocalDate.parse("1990-09-29")
                )
            )
        )

        wiremock.mockOmsorgspengerInfotrygdRammevedtakHentRammevedtak(
            identitetsnummer = identitetsnummer,
            periode = periode
        )

        val infotrygdRammer = gateway.hent(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = UUID.randomUUID().toString()
        )

        assertEquals(8, infotrygdRammer.size)
        assertThat(infotrygdRammer).containsAll(forventedeInfotrygdRammer)
    }

    @Test
    fun `Helsesjekk for å hente ramemvedtak fra infotrygd`() {
        runBlocking { gateway.check() }.also { result ->
            assertTrue(result is Healthy)
        }
    }

    private val wiremock = WireMockBuilder()
        .withAzureSupport()
        .build()
        .mockOmsorgspengerInfotrygdRammevedtakIsReady()

    private val gateway = OmsorgspengerInfotrygdRammevedtakGateway(
        accessTokenClient = ClientSecretAccessTokenClient(
            clientId = "omsorgspenger-rammemeldinger",
            clientSecret = "secret",
            tokenEndpoint = URI(wiremock.getAzureV2TokenUrl())
        ),
        hentRammevedtakFraInfotrygdScopes = setOf("omsorgspenger-infotrygd-rammevedtak/.default"),
        omsorgspengerInfotrygdRammevedtakBaseUrl = URI(wiremock.omsorgspengerInfotrygdRammevedtakBaseUrl())
    )

    @AfterAll
    fun afterAll() {
        wiremock.stop()
    }
}