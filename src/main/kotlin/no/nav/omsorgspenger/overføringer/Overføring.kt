package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.Saksreferanse

internal data class Overføring(
    val antallDager: Int,
    val periode: Periode,
    val starterGrunnet: Set<Knekkpunkt>,
    val slutterGrunnet: Set<Knekkpunkt>
)

internal fun List<Overføring>.gitt(til: Saksreferanse) = map { OverføringGitt(
    antallDager = it.antallDager,
    periode = it.periode,
    til = til
)}

internal fun List<Overføring>.fått(fra: Saksreferanse) = map { OverføringFått(
    antallDager = it.antallDager,
    periode = it.periode,
    fra = fra
)}

internal data class OverføringGitt(
    val antallDager: Int,
    val periode: Periode,
    val til: Saksreferanse
)

internal data class OverføringFått(
    val antallDager: Int,
    val periode: Periode,
    val fra: Saksreferanse
)

internal data class GjeldendeOverføringer(
    val saksnummer: Saksnummer,
    val gitt: List<OverføringGitt> = listOf(),
    val fått: List<OverføringFått> = listOf()
)

internal fun Map<KnektPeriode, Int>.somOverføringer(
    ønsketOmsorgsdagerÅOverføre: Int
) : List<Overføring> {
    val overføringer = mutableListOf<Overføring>()

    forEach { (knektPeriode, omsorgsdagerTilgjengeligForOverføring) ->
        val antallDager = minOf(omsorgsdagerTilgjengeligForOverføring, ønsketOmsorgsdagerÅOverføre)
        val overføring = overføringer.firstOrNull { it.antallDager == antallDager && it.periode.erKantIKant(knektPeriode.periode) }

        when (overføring) {
            null -> {
                overføringer.add(Overføring(
                    antallDager = antallDager,
                    periode = knektPeriode.periode,
                    starterGrunnet = knektPeriode.starterGrunnet,
                    slutterGrunnet = knektPeriode.slutterGrunnet
                ))
            }
            else -> {
                overføringer.remove(overføring)
                overføringer.add(Overføring(
                    antallDager = antallDager,
                    periode = knektPeriode.periode.slåSammen(overføring.periode),
                    // 'starterGrunnet' beholdes
                    // 'slutterGrunnet' erstattes
                    starterGrunnet = overføring.starterGrunnet,
                    slutterGrunnet = knektPeriode.slutterGrunnet
                ))
            }
        }
    }
    return overføringer.filterNot { it.antallDager == 0 }.toList()
}