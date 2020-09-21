package no.nav.omsorgspenger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PeriodiseringTest {

    @Test
    fun `Ingen eller en dato`() {
        assertPerioder(listOf(), listOf())
        assertPerioder(listOf(LocalDate.parse("2019-12-30")), listOf(Periode("2019-12-30/2019-12-30")))
    }

    @Test
    fun `Datoer kant i kant skal kun gi perioder på enkeltdager`() {
        val datoer = listOf(
            LocalDate.parse("2019-12-30"),
            LocalDate.parse("2019-12-31"),
            LocalDate.parse("2020-01-01"),
            LocalDate.parse("2020-01-02"),
            LocalDate.parse("2020-01-03")
        )

        val forventedePerioder = listOf(
            Periode("2019-12-30/2019-12-30"),
            Periode("2019-12-31/2019-12-31"),
            Periode("2020-01-01/2020-01-01"),
            Periode("2020-01-02/2020-01-03")
        )

        assertPerioder(datoer, forventedePerioder)
    }

    @Test
    fun `Datoer med mellomrom mellom`() {
        val datoer = listOf(
            LocalDate.parse("2019-12-30"),
            LocalDate.parse("2022-01-10"),
            LocalDate.parse("2015-05-12"),
            LocalDate.parse("2015-05-13"),
            LocalDate.parse("2015-05-14"),
            LocalDate.parse("2013-01-01")
        )

        val forventedePerioder = listOf(
            Periode("2013-01-01/2015-05-11"),
            Periode("2015-05-12/2015-05-12"),
            Periode("2015-05-13/2015-05-13"),
            Periode("2015-05-14/2019-12-29"),
            Periode("2019-12-30/2022-01-10")
        )

        assertPerioder(datoer, forventedePerioder)
    }

    private fun assertPerioder(datoer: List<LocalDate>, forventedePerioder: List<Periode>) {
        println("Datoer=${datoer.sorted()}")
        println("ForventedePerioder=${forventedePerioder}")
        val perioder = datoer.periodiser()
        println("Perioder=${perioder}")
        assertEquals(forventedePerioder, perioder)
    }

}