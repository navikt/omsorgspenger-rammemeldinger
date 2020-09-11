package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import org.junit.jupiter.api.Assertions.assertEquals
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