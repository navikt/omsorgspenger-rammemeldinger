package no.nav.omsorgspenger.overføringer

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.koronaoverføringer.apis.SpleisetKoronaOverføringerService
import no.nav.omsorgspenger.overføringer.apis.Motpart
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringer
import no.nav.omsorgspenger.overføringer.statistikk.StatistikkFormat.assertForventetGjennomført
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.RecordingStatistikkService
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class TidligereKoronaoverførtTest(
    dataSource: DataSource) {

    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate()
    ).also { builder ->
        builder.spleisetKoronaOverføringerService = spleisetKoronaOverføringerServiceMock
    }.build()

    private val statistikkService = applicationContext.statistikkService as RecordingStatistikkService

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContext)
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Får overført færre dager i 2021 grunnet koronaoverføring`() {
        val fra = IdentitetsnummerGenerator.identitetsnummer()
        val til = IdentitetsnummerGenerator.identitetsnummer()
        val id = "01EVXYA9JTY4W0F49MA8ZJAT8M"

        val barn = overføreOmsorgsdagerBarn(
            aleneOmOmsorgen = true,
            fødselsdato = LocalDate.parse("2018-09-29")
        )
        val (behovssekvensId, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            id = id,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 9,
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = LocalDate.parse("2021-03-01"),
            barn = listOf(barn)
        )

        assertEquals(id, behovssekvensId)

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til,
            barn = setOf(barn.identitetsnummer),
            borsammen = true
        )

        val (løsningId, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertEquals(id, løsningId)
        assertTrue(løsning.erGjennomført())
        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            forventedeOverføringer = mapOf(
                Periode("2021-03-01/2021-12-31") to 2,
                Periode("2022-01-01/2030-12-31") to 9
            )
        )

        statistikkService.finnStatistikkMeldingFor(behovssekvensId).assertForventetGjennomført(behovssekvensId)
    }

    private companion object {
        val spleisetKoronaOverføringerServiceMock = mockk<SpleisetKoronaOverføringerService>().also {
            every { it.hentSpleisetOverføringer(any(), any(), any()) }.returns(SpleisetOverføringer(
                fått = emptyList(),
                gitt = listOf(SpleisetOverføringGitt(
                    gjennomført = LocalDate.now(),
                    gyldigFraOgMed = LocalDate.parse("2021-01-01"),
                    gyldigTilOgMed = LocalDate.parse("2021-12-31"),
                    til = Motpart(id = "foo", type = "bar"),
                    lengde = Duration.ofDays(10),
                    kilder = emptySet()
                ), SpleisetOverføringGitt(
                    gjennomført = LocalDate.now(),
                    gyldigFraOgMed = LocalDate.parse("2021-02-10"),
                    gyldigTilOgMed = LocalDate.parse("2021-12-31"),
                    til = Motpart(id = "foo", type = "bar"),
                    lengde = Duration.ofDays(28),
                    kilder = emptySet()
                ))
            ))
        }
    }
}