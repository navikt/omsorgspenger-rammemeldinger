package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode

internal data class Overføring(
    internal val antallDager: Int,
    internal val periode: Periode
) {
    internal fun somLøsning() = mapOf(
        "antallDager" to antallDager,
        "periode" to periode.toString()
    )
}

internal fun Map<Periode, Int>.somOverføringer() : List<Overføring> {
    val overføringer = mutableListOf<Overføring>()

    forEach { (periode, omsorgsdagerTilgjengeligForOverføring) ->
        val overføring = overføringer.firstOrNull { it.antallDager == omsorgsdagerTilgjengeligForOverføring && it.periode.erKantIKant(periode) }

        when (overføring) {
            null -> {
                overføringer.add(Overføring(
                    antallDager = omsorgsdagerTilgjengeligForOverføring,
                    periode = periode
                ))
            }
            else -> {
                overføringer.remove(overføring)
                overføringer.add(Overføring(
                    antallDager = omsorgsdagerTilgjengeligForOverføring,
                    periode = periode.slåSammen(overføring.periode)
                ))
            }
        }
    }
    return overføringer.toList()
}