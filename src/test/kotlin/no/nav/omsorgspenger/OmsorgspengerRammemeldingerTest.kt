package no.nav.omsorgspenger

import de.huxhorn.sulky.ulid.ULID
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
    fun `Tar emot gyldigt behov `() {

        val (id, overføring) = Behovssekvens(
                id = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                correlationId = UUID.randomUUID().toString(),
                behov = arrayOf(OverføreOmsorgsdagerBehov(
                        fra = OverføreOmsorgsdagerBehov.OverførerFra(
                                identitetsnummer = "11111111111",
                                borINorge = true,
                                jobberINorge = true
                        ),
                        til = OverføreOmsorgsdagerBehov.OverførerTil(
                                identitetsnummer = "11111111112",
                                relasjon = OverføreOmsorgsdagerBehov.Relasjon.NåværendeSamboer,
                                harBoddSammenMinstEttÅr = false
                        ),
                        omsorgsdagerTattUtIÅr = 10,
                        omsorgsdagerÅOverføre = 5,
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

        rapid.sendTestMessage(overføring)

        assertEquals(1, rapid.inspektør.size)

        assertTrue(rapid.inspektør.message(0).toString().somMelding().harLøsningPå(OverføreOmsorgsdagerLøsningResolver.Instance))

        val løsning = rapid.inspektør.message(0).toString().somMelding()
                .løsningPå(OverføreOmsorgsdagerLøsningResolver.Instance)

        assertNotNull(løsning)

    }


    @Test
    fun `Tar ikke emot ugyldigt behov`() {
        val ugyldigtBehov = """
            {"@id":"01ARZ3NDEKTSV4RRFFQ69G5FAV","@type":"error"}
        """.trimIndent()

        rapid.sendTestMessage(ugyldigtBehov)

        assertEquals(0, rapid.inspektør.size)
    }

}