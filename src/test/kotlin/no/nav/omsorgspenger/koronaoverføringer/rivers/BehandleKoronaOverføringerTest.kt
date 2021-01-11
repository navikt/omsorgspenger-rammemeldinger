package no.nav.omsorgspenger.koronaoverføringer.rivers

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgen
import no.nav.omsorgspenger.koronaoverføringer.TestVerktøy.overføring
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringer
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringerService
import no.nav.omsorgspenger.registerOverføreKoronaOmsorgsdager
import no.nav.omsorgspenger.testutils.*
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(DataSourceExtension::class)
internal class BehandleKoronaOverføringerTest(
    dataSource: DataSource) {
    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate(),
        additionalEnv = mapOf("KORONA_BEHANDLING" to "enabled")
    ).also { builder ->
        builder.spleisetOverføringerService = overføringerMock
    }.build()

    private val rapid = TestRapid().apply {
        this.registerOverføreKoronaOmsorgsdager(applicationContext)
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Gjennomført overføring`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()

        val barnetsFødselsdato = LocalDate.parse("2020-10-05")
        val barnet = koronaBarn(fødselsdato = barnetsFødselsdato, aleneOmOmsorgen = true)

        val (idStart, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            barn = listOf(barnet)
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)
        rapid.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner(
            fra = fra,
            til = til,
            barn = setOf(barnet.identitetsnummer),
            borSammen = true
        )
        rapid.ventPå(2)

        rapid.mockHentPersonopplysninger(
            fra = fra,
            til = til
        )
        rapid.ventPå(3)
        val (idSlutt, løsning) = rapid.løsningOverføreKoronaOmsorgsdager()
        assertTrue(løsning.erGjennomført())
        assertEquals(idStart, idSlutt)

        hentKoronaoverføringerFor(fra).also {
            assertThat(it.fått).isEmpty()
            assertThat(it.gitt).hasSize(1)
        }
        hentKoronaoverføringerFor(til).also {
            assertThat(it.fått).hasSize(1)
            assertThat(it.gitt).isEmpty()
        }
        hentAleneOmOmsorgen("foo").also {
            assertThat(it).hasSameElementsAs(setOf(AleneOmOmsorgen(
                registrert = registrert,
                periode = Periode(fom = LocalDate.now(), tom = LocalDate.parse("2038-12-31")), // Ut året barnet fyller 18
                behovssekvensId = idStart,
                barn = AleneOmOmsorgen.Barn(identitetsnummer = barnet.identitetsnummer, fødselsdato = barnet.fødselsdato),
                regstrertIForbindelseMed = "KoronaOverføring"
            )))
        }
    }

    @Test
    fun `Avslått overføring ifall alle barn ikke bor på samme adresse`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val barn = koronaBarn()

        val (idStart, behovssekvens) = behovssekvensOverføreKoronaOmsorgsdager(
            fra = fra,
            til = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            barn = listOf(barn)
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.ventPå(1)
        rapid.mockHentOmsorgspengerSaksnummerOchVurderRelasjoner(
            fra = fra,
            til = til,
            barn = setOf(barn.identitetsnummer),
            borSammen = false
        )
        rapid.ventPå(2)

        rapid.mockHentPersonopplysninger(
            fra = fra,
            til = til
        )
        rapid.ventPå(3)
        val (idSlutt, løsning) = rapid.løsningOverføreKoronaOmsorgsdager()
        assertTrue(løsning.erAvslått())
        assertEquals(idStart, idSlutt)
    }

    private fun hentKoronaoverføringerFor(identitetsnummer: Identitetsnummer) =
        applicationContext.spleisetKoronaOverføringerService.hentSpleisetOverføringer(
            identitetsnummer = identitetsnummer,
            periode = Periode("2021-01-01/2021-12-31"),
            correlationId = "test"
        )

    private fun hentAleneOmOmsorgen(saksnummer: Saksnummer) = applicationContext.aleneOmOmsorgenRepository.hent(saksnummer).map {
        it.copy(registrert = registrert)
    }

    private companion object {
        val registrert = ZonedDateTime.parse("2021-01-10T12:15:00.000+01:00")
        private val overføringerMock = mockk<SpleisetOverføringerService>().also {
            every { it.hentSpleisetOverføringer(any(), any(), any()) }
                .returns(SpleisetOverføringer(
                    gitt = listOf(overføring(
                        periode = Periode("2021-01-01/2021-12-31"),
                        antallDager = 7
                    )),
                    fått = emptyList()
                ))
        }
    }
}