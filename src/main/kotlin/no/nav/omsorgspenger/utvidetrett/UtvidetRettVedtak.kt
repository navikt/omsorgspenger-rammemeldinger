package no.nav.omsorgspenger.utvidetrett

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.Periode
import java.time.LocalDate

internal data class UtvidetRettVedtak(
    val periode: Periode,
    val barnetsFødselsdato: LocalDate,
    val barnetsIdentitetsnummer: String? = null
) {
    internal fun somLøsning() = mapOf(
        "periode" to periode.toString(),
        "barnetsFødselsdato" to barnetsFødselsdato.toString(),
        "barnetsIdentitetsnummer" to barnetsIdentitetsnummer
    )

    internal companion object {
        internal fun JsonNode.erUtvidetRettVedtak() = try {
            somUtvidetRettVedtak()
            true
        } catch (cause: Throwable) { false }

        internal fun JsonNode.somUtvidetRettVedtak() = UtvidetRettVedtak(
            periode = Periode(get("periode").asText()),
            barnetsFødselsdato = get("barnetsFødselsdato").asLocalDate(),
            barnetsIdentitetsnummer = when (hasNonNull("barnetsIdentitetsnummer")) {
                true -> get("barnetsIdentitetsnummer").asText()
                false -> null
            }
        )
    }
}

internal fun List<UtvidetRettVedtak>.somLøsning() = mapOf(
    "vedtak" to this.map { it.somLøsning() }
)