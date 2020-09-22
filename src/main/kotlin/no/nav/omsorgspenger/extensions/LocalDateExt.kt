package no.nav.omsorgspenger.extensions

import java.time.LocalDate

internal fun LocalDate.erFørEllerLik(annen: LocalDate) = isBefore(annen) || isEqual(annen)
internal fun LocalDate.førsteDagNesteÅr() = withYear(year+1).withMonth(1).withDayOfMonth(1)
internal fun LocalDate.sisteDagIÅret() = withMonth(12).withDayOfMonth(31)