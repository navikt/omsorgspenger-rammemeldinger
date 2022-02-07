package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode

internal data class NyOverføring(
    val antallDager: Int,
    val periode: Periode,
    val starterGrunnet: List<Knekkpunkt>,
    val slutterGrunnet: List<Knekkpunkt>
)

internal fun Map<KnektPeriode, Int>.somNyeOverføringer(
    ønsketOmsorgsdagerÅOverføre: Int
) : List<NyOverføring> {
    val overføringer = mutableListOf<NyOverføring>()

    forEach { (knektPeriode, omsorgsdagerTilgjengeligForOverføring) ->
        val antallDager = minOf(
            omsorgsdagerTilgjengeligForOverføring,
            ønsketOmsorgsdagerÅOverføre).also { require(it >= 0) }
        
        val overføring = overføringer.firstOrNull { it.antallDager == antallDager && it.periode.erKantIKant(knektPeriode.periode) }
        when (overføring) {
            null -> {
                overføringer.add(NyOverføring(
                    antallDager = antallDager,
                    periode = knektPeriode.periode,
                    starterGrunnet = knektPeriode.starterGrunnet,
                    slutterGrunnet = knektPeriode.slutterGrunnet
                ))
            }
            else -> {
                overføringer.remove(overføring)
                overføringer.add(NyOverføring(
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
    return overføringer
}

internal fun List<NyOverføring>.fjernOverføringerUtenDager() = filterNot { it.antallDager <= 0 }