package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsningResolver
import no.nav.k9.rapid.losning.somMelding
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OmsorgspengerRammemeldingerTest {

    val rapid = TestRapid().apply {
        OmsorgspengerRammemeldinger(this)
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Gyldig behov som gjennomføres `() {
        val id = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
        val fra = "11111111111"
        val til = "11111111112"
        val omsorgsdagerÅOverføre = 5

        val (behovssekvensId, behovssekvens) = behovssekvens(
            id = id,
            fra = fra,
            til = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
        )

        assertEquals(id, behovssekvensId)

        rapid.sendTestMessage(behovssekvens)

        assertEquals(1, rapid.inspektør.size)

        assertTrue(rapid.inspektør.message(0).toString().somMelding().harLøsningPå(OverføreOmsorgsdagerLøsningResolver.Instance))

        val (løsningId, løsning) = rapid.inspektør
            .message(0)
            .toString()
            .somMelding()
            .løsningPå(OverføreOmsorgsdagerLøsningResolver.Instance)

        assertEquals(id, løsningId)
        assertTrue(løsning.erGjennomført())
        assertTrue(løsning.overføringer.containsKey(fra))
        assertTrue(løsning.overføringer.containsKey(til))
        // Personen overføringen er fra har gitt 1 og fått 0 overføringer
        assertEquals(1, løsning.overføringer.getValue(fra).gitt.size)
        assertEquals(omsorgsdagerÅOverføre, løsning.overføringer.getValue(fra).gitt.first().antallDager)
        assertEquals(0, løsning.overføringer.getValue(fra).fått.size)
        // Personen overføringen er til har fått 1 og gitt 0 overføringer
        assertEquals(0, løsning.overføringer.getValue(til).gitt.size)
        assertEquals(1, løsning.overføringer.getValue(til).fått.size)
        assertEquals(omsorgsdagerÅOverføre, løsning.overføringer.getValue(til).fått.first().antallDager)
    }
    

    @Test
    fun `Melding som ikke inneholder behov for å overføre omsorgsdager`() {
        val ugyldigtBehov = """
            {"@id":"01ARZ3NDEKTSV4RRFFQ69G5FAV","@type":"error"}
        """.trimIndent()

        rapid.sendTestMessage(ugyldigtBehov)

        assertEquals(0, rapid.inspektør.size)
    }

    internal companion object {
        private fun behovssekvens(
            id: String,
            fra: String,
            til: String,
            omsorgsdagerTattUtIÅr: Int,
            omsorgsdagerÅOverføre: Int
        ) = Behovssekvens(
            id = id,
            correlationId = UUID.randomUUID().toString(),
            behov = arrayOf(OverføreOmsorgsdagerBehov(
                fra = OverføreOmsorgsdagerBehov.OverførerFra(
                    identitetsnummer = fra,
                    borINorge = true,
                    jobberINorge = true
                ),
                til = OverføreOmsorgsdagerBehov.OverførerTil(
                    identitetsnummer = til,
                    relasjon = OverføreOmsorgsdagerBehov.Relasjon.NåværendeSamboer,
                    harBoddSammenMinstEttÅr = true
                ),
                omsorgsdagerTattUtIÅr = omsorgsdagerTattUtIÅr,
                omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
                barn = listOf(OverføreOmsorgsdagerBehov.Barn(
                    identitetsnummer = "11111111113",
                    fødselsdato = LocalDate.now(),
                    aleneOmOmsorgen = true,
                    utvidetRett = false
                )),
                kilde = OverføreOmsorgsdagerBehov.Kilde.Brev,
                journalpostIder = listOf()
            ))
        ).keyValue
    }

}