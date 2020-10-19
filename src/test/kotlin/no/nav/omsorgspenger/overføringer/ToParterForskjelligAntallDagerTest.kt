package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.IdentitetsnummerGenerator.identitetsnummer
import no.nav.omsorgspenger.registerApplicationContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ToParterForskjelligAntallDagerTest {

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(TestAppliationContextBuilder().build())
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Overfører dager avsender har tilgjengelig`() {
        val fra = identitetsnummer()
        val til = identitetsnummer()
        val id = "01EJ6M5B1YF1EGFABH2WC57KDC"
        val omsorgsdagerÅOverføre = 5

        val (behovssekvensId, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            id = id,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
            overføringFra = fra,
            overføringTil = til,
            mottaksdato = LocalDate.parse("2020-09-29"),
            barn = listOf(overføreOmsorgsdagerBarn(
                aleneOmOmsorgen = true,
                fødselsdato = LocalDate.parse("2018-09-29")
            ))
        )

        assertEquals(id, behovssekvensId)

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til
        )

        val (løsningId, løsning) = rapid.løsningOverføreOmsorgsdager()

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
        val fra = identitetsnummer()
        val til = identitetsnummer()
        val id = "01EJ6M744H38HJCJVMKEJPQ9KP"
        val omsorgsdagerÅOverføre = 5

        val (behovssekvensId, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            id = id,
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 17,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
            mottaksdato = LocalDate.parse("2020-09-29"),
            barn = listOf(overføreOmsorgsdagerBarn(
                aleneOmOmsorgen = true,
                fødselsdato = LocalDate.parse("2018-09-29")
            ))
        )

        assertEquals(id, behovssekvensId)

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til
        )

        val (løsningId, løsning) = rapid.løsningOverføreOmsorgsdager()

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
        val fra = identitetsnummer()
        val til = identitetsnummer()
        val id = "01EJ6M7E83DJQQR5ABS0XAKTC6"
        val omsorgsdagerÅOverføre = 11

        val (behovssekvensId, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            id = id,
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre,
            mottaksdato = LocalDate.parse("2020-09-29"),
            barn = listOf(overføreOmsorgsdagerBarn(
                aleneOmOmsorgen = true,
                fødselsdato = LocalDate.parse("2018-09-29")
            ))
        )

        assertEquals(id, behovssekvensId)

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til
        )

        val (løsningId, løsning) = rapid.løsningOverføreOmsorgsdager()

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
}