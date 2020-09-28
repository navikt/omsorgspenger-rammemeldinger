package no.nav.omsorgspenger.fordelinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.omsorgspenger.Periode

internal data class FordelingGirMelding(
    internal val periode: Periode,
    internal val antallDager: Int
) {
    internal fun somLøsning() = mapOf(
        "periode" to periode.toString(),
        "antallDager" to antallDager
    )

    internal companion object {
        internal fun JsonNode.erFordelingGirMelding() = try {
            somFordelingGirMelding()
            true
        } catch (cause: Throwable) { false }

        internal fun JsonNode.somFordelingGirMelding() = FordelingGirMelding(
            periode = Periode(get("periode").asText()),
            antallDager = get("antallDager").asInt()
        )
    }
}

internal fun List<FordelingGirMelding>.somLøsning() = mapOf(
    "meldinger" to this.map { it.somLøsning() }
)