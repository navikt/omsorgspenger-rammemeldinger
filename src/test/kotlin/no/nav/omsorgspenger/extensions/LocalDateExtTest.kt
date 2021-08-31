package no.nav.omsorgspenger.extensions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

internal class LocalDateExtTest {

    @Test
    fun `Test toLocalDateOslo i overgangen til ny dag`() {
        val zonedDateTime = ZonedDateTime.parse("2020-11-10T23:00:00.00Z")
        assertEquals(LocalDate.parse("2020-11-11"), zonedDateTime.toLocalDateOslo())
    }

    @Test
    fun `Test toLocalDateOslo midt på dagen`() {
        val zonedDateTime = ZonedDateTime.parse("2020-11-10T12:00:00.00Z")
        assertEquals(LocalDate.parse("2020-11-10"), zonedDateTime.toLocalDateOslo())
    }
}