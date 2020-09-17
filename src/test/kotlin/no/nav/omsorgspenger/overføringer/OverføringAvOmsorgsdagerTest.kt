package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsning
import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsningResolver
import no.nav.k9.rapid.losning.somMelding
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
        StartOverføringAvOmsorgsdager(this)
        FerdigstillOverføringAvOmsorgsdager(this)
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
        rapid.mockLøsningPåHenteOmsorgspengerSaksnummer(saksnummer = "123")
        ventPå(antallMeldinger = 2)

        val (løsningId, løsning) = rapid.løsning()

        assertEquals(id, løsningId)
        assertTrue(løsning.erGjennomført())
        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
        )
    }

    @Test
    fun `Forsøker å overføre fler dager enn avsender har tilgjengelig`() {
        val id = "01EJ6M744H38HJCJVMKEJPQ9KP"
        val omsorgsdagerÅOverføre = 5

        val (behovssekvensId, behovssekvens) = behovssekvens(
            id = id,
            omsorgsdagerTattUtIÅr = 6,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
        )

        assertEquals(id, behovssekvensId)

        rapid.sendTestMessage(behovssekvens)

        val (løsningId, løsning) = rapid.løsning()

        assertEquals(id, løsningId)
        assertTrue(løsning.erAvslått())

        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
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

        val (løsningId, løsning) = rapid.løsning()

        assertEquals(id, løsningId)
        assertTrue(løsning.erAvslått())

        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
        )
    }

    @Test
    fun `Overfører dager med utvidet rett på barn`() {
        val id = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
        val omsorgsdagerÅOverføre = 5

        val (behovssekvensId, behovssekvens) = behovssekvens(
            id = id,
            omsorgsdagerTattUtIÅr = 0,
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
        omsorgsdagerÅOverføre: Int) {
        // Skal inneholde overføringer for begge parter
        assertTrue(containsKey(fra))
        assertTrue(containsKey(til))
        // Personen overføringen er fra har gitt 1 og fått 0 overføringer
        assertEquals(1, getValue(fra).gitt.size)
        assertEquals(omsorgsdagerÅOverføre,getValue(fra).gitt.first().antallDager)
        assertEquals(0, getValue(fra).fått.size)
        // Personen overføringen er til har fått 1 og gitt 0 overføringer
        assertEquals(0, getValue(til).gitt.size)
        assertEquals(1, getValue(til).fått.size)
        assertEquals(omsorgsdagerÅOverføre, getValue(til).fått.first().antallDager)
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
                    fødselsdato = LocalDate.now(),
                    aleneOmOmsorgen = true,
                    utvidetRett = utvidetRett
                )),
                kilde = OverføreOmsorgsdagerBehov.Kilde.Brev,
                journalpostIder = listOf()
            ))
        ).keyValue
    }

}