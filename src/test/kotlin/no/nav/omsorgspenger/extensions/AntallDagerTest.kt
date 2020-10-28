package no.nav.omsorgspenger.extensions

import no.nav.omsorgspenger.extensions.AntallDager.antallDager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

internal class AntallDagerTest {

    @Test
    fun `hele dager`() {
        val tiDager = Duration.ofDays(10)
        val gjennomAntallDager = Duration.ofDays(tiDager.antallDager().toLong())
        assertEquals(tiDager, gjennomAntallDager)
    }

    @Test
    fun `halve dager`() {
        val tiDager = Duration.ofDays(10)
        val gjennomAntallDager = Duration.ofDays(tiDager
            .plusHours(12)
            .antallDager()
            .toLong()
        )
        assertEquals(tiDager.plusDays(1), gjennomAntallDager)
    }

    @Test
    fun `negative dager`() {
        val minusTreDager = Duration.ZERO.minusDays(3)
        val gjennomAntallDager = Duration.ofDays(minusTreDager.antallDager().toLong())
        assertEquals(Duration.ZERO, gjennomAntallDager)
    }
}