package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.omsorgspenger.Periode

internal data class Overføring(
    internal val antallDager: Int,
    internal val periode: Periode
) {
    internal fun somLøsning() = mapOf(
        "antallDager" to antallDager,
        "periode" to periode.toString()
    )

    internal companion object {
        internal fun JsonNode.erOverføring() = try {
            somOverføring()
            true
        } catch (cause: Throwable) { false }
        internal fun JsonNode.somOverføring() = Overføring(
            antallDager = get("antallDager").asInt(),
            periode = Periode(get("periode").asText())
        )
    }
}

internal fun Map<Periode, Int>.somOverføringer(
    ønsketOmsorgsdagerÅOverføre: Int
) : List<Overføring> {
    val overføringer = mutableListOf<Overføring>()

    forEach { (periode, omsorgsdagerTilgjengeligForOverføring) ->
        val antallDager = minOf(omsorgsdagerTilgjengeligForOverføring, ønsketOmsorgsdagerÅOverføre)
        val overføring = overføringer.firstOrNull { it.antallDager == antallDager && it.periode.erKantIKant(periode) }

        when (overføring) {
            null -> {
                overføringer.add(Overføring(
                    antallDager = antallDager,
                    periode = periode
                ))
            }
            else -> {
                overføringer.remove(overføring)
                overføringer.add(Overføring(
                    antallDager = antallDager,
                    periode = periode.slåSammen(overføring.periode)
                ))
            }
        }
    }
    return overføringer.filterNot { it.antallDager == 0 }.toList()
}