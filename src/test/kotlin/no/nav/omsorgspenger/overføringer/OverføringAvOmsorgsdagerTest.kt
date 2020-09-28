package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsning
import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsningResolver
import no.nav.k9.rapid.losning.somMelding
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.medAlleRivers
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OverføringAvOmsorgsdagerTest {

    private val rapid = TestRapid().apply {
        medAlleRivers()
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Overfører dager avsender har tilgjengelig`() {
        val id = "01EJ6M5B1YF1EGFABH2WC57KDC"
        val omsorgsdagerÅOverføre = 5

        val (behovssekvensId, behovssekvens) = behovssekvens(
            id = id,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
        )

        assertEquals(id, behovssekvensId)

        rapid.sendTestMessage(behovssekvens)
        ventPå(antallMeldinger = 1)
        rapid.mockLøsningPåHentePersonopplysninger(
            fra = fra,
            til = til
        )
        ventPå(antallMeldinger = 2)

        val (løsningId, løsning) = rapid.løsning()

        assertEquals(id, løsningId)
        assertTrue(løsning.erGjennomført())
        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            forventedeOverføringer = mapOf(
                Periode("2020-09-29/2030-12-31") to omsorgsdagerÅOverføre
            )
        )
    }

    @Test
    fun `Forsøker å overføre fler dager enn avsender har tilgjengelig`() {
        val id = "01EJ6M744H38HJCJVMKEJPQ9KP"
        val omsorgsdagerÅOverføre = 5

        val (behovssekvensId, behovssekvens) = behovssekvens(
            id = id,
            omsorgsdagerTattUtIÅr = 17,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
        )

        assertEquals(id, behovssekvensId)

        rapid.sendTestMessage(behovssekvens)
        ventPå(antallMeldinger = 1)
        rapid.mockLøsningPåHentePersonopplysninger(
            fra = fra,
            til = til
        )
        ventPå(antallMeldinger = 2)

        val (løsningId, løsning) = rapid.løsning()

        assertEquals(id, løsningId)
        assertTrue(løsning.erGjennomført())

        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            forventedeOverføringer = mapOf(
                Periode("2020-09-29/2020-12-31") to 3,
                Periode("2021-01-01/2030-12-31") to 5
            )
        )
    }

    @Test
    fun `Forsøker å overføre fler enn 10 dager`() {
        val id = "01EJ6M7E83DJQQR5ABS0XAKTC6"
        val omsorgsdagerÅOverføre = 11

        val (behovssekvensId, behovssekvens) = behovssekvens(
            id = id,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
        )

        assertEquals(id, behovssekvensId)

        rapid.sendTestMessage(behovssekvens)
        ventPå(antallMeldinger = 1)
        rapid.mockLøsningPåHentePersonopplysninger(
            fra = fra,
            til = til
        )
        ventPå(antallMeldinger = 2)
        val (løsningId, løsning) = rapid.løsning()

        assertEquals(id, løsningId)
        assertTrue(løsning.erGjennomført())

        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            forventedeOverføringer = mapOf(
                Periode("2020-09-29/2030-12-31") to 10
            )
        )
    }

    @Test
    fun `Overfører dager med utvidet rett på barn som ikke kan verifiseres`() {
        val id = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
        val omsorgsdagerÅOverføre = 5

        val (behovssekvensId, behovssekvens) = behovssekvens(
            id = id,
            omsorgsdagerTattUtIÅr = 16,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
            utvidetRett = true
        )

        assertEquals(id, behovssekvensId)

        rapid.sendTestMessage(behovssekvens)

        val (løsningId, løsning) = rapid.løsning()

        assertEquals(id, løsningId)
        assertTrue(løsning.ikkeBehandlesAvNyttSystem())
        assertTrue(løsning.overføringer.isEmpty())
    }

    @Test
    fun `Melding som ikke inneholder behov for å overføre omsorgsdager`() {
        val ugyldigtBehov = """
            {"@id":"01ARZ3NDEKTSV4RRFFQ69G5FAV","@type":"error"}
        """.trimIndent()

        rapid.sendTestMessage(ugyldigtBehov)

        assertEquals(0, rapid.inspektør.size)
    }

    private fun Map<String, OverføreOmsorgsdagerLøsning.Overføringer>.assertOverføringer(
        fra: String,
        til: String,
        forventedeOverføringer: Map<Periode, Int>) {
        // Skal inneholde overføringer for begge parter
        assertTrue(containsKey(fra))
        assertTrue(containsKey(til))
        // Personen overføringen er fra har gitt X og fått 0 overføringer
        assertEquals(forventedeOverføringer.size, getValue(fra).gitt.size)
        forventedeOverføringer.forEach { (periode, antallDager) ->
            assertNotNull(getValue(fra).gitt.firstOrNull { it.antallDager == antallDager && periode == Periode(fom = it.gjelderFraOgMed, tom = it.gjelderTilOgMed)})
        }
        assertEquals(0, getValue(fra).fått.size)
        // Personen overføringen er til har fått X og gitt 0 overføringer
        assertEquals(0, getValue(til).gitt.size)
        assertEquals(forventedeOverføringer.size, getValue(til).fått.size)
        forventedeOverføringer.forEach { (periode, antallDager) ->
            assertNotNull(getValue(til).fått.firstOrNull { it.antallDager == antallDager && periode == Periode(fom = it.gjelderFraOgMed, tom = it.gjelderTilOgMed)})
        }
    }

    private fun TestRapid.løsning() = sisteMelding().somMelding().løsningPå(OverføreOmsorgsdagerLøsningResolver.Instance)

    private fun ventPå(antallMeldinger: Int) = await().atMost(Duration.ofSeconds(1)).until { rapid.inspektør.size == antallMeldinger }

    internal companion object {
        private val fra = "11111111111"
        private val til = "11111111112"

        private fun behovssekvens(
            id: String,
            overføringFra: String = fra,
            overføringTil: String = til,
            omsorgsdagerTattUtIÅr: Int,
            omsorgsdagerÅOverføre: Int,
            mottaksdato: LocalDate = LocalDate.parse("2020-09-29"),
            utvidetRett: Boolean = false
        ) = Behovssekvens(
            id = id,
            correlationId = UUID.randomUUID().toString(),
            behov = arrayOf(OverføreOmsorgsdagerBehov(
                fra = OverføreOmsorgsdagerBehov.OverførerFra(
                    identitetsnummer = overføringFra,
                    borINorge = true,
                    jobberINorge = true
                ),
                til = OverføreOmsorgsdagerBehov.OverførerTil(
                    identitetsnummer = overføringTil,
                    relasjon = OverføreOmsorgsdagerBehov.Relasjon.NåværendeSamboer,
                    harBoddSammenMinstEttÅr = true
                ),
                omsorgsdagerTattUtIÅr = omsorgsdagerTattUtIÅr,
                omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
                barn = listOf(OverføreOmsorgsdagerBehov.Barn(
                    identitetsnummer = "11111111113",
                    fødselsdato = mottaksdato.minusYears(2),
                    aleneOmOmsorgen = true,
                    utvidetRett = utvidetRett
                )),
                kilde = OverføreOmsorgsdagerBehov.Kilde.Brev,
                journalpostIder = listOf(),
                mottaksdato = mottaksdato
            ))
        ).keyValue
    }

}