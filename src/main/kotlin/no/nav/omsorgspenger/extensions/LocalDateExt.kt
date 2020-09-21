package no.nav.omsorgspenger.extensions

import no.nav.omsorgspenger.Periode
import java.time.LocalDate

internal fun LocalDate.erFørEllerLik(annen: LocalDate) = isBefore(annen) || isEqual(annen)
internal fun LocalDate.førsteDagNesteÅr() = withMonth(year+1).withMonth(1).withDayOfMonth(1)
internal fun LocalDate.sisteDagIÅret() = withMonth(12).withDayOfMonth(31)
internal fun MutableCollection<LocalDate>.leggTil(periode: Periode) {
    add(periode.fom)
    add(periode.tom)
}