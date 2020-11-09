package no.nav.omsorgspenger.extensions

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private val Oslo = ZoneId.of("Europe/Oslo")
internal fun LocalDate.erFørEllerLik(annen: LocalDate) = isBefore(annen) || isEqual(annen)
internal fun LocalDate.førsteDagNesteÅr() = withYear(year+1).withMonth(1).withDayOfMonth(1)
internal fun LocalDate.sisteDagIÅret() = withMonth(12).withDayOfMonth(31)
internal fun ZonedDateTime.toLocalDateOslo() = withZoneSameInstant(Oslo).toLocalDate() // TODO: Test