package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.Identitetsnummer
import java.time.LocalDate

internal data class Part(
    internal val identitetsnummer: Identitetsnummer,
    internal val fødselsdato: LocalDate,
    internal val navn: String
) {
    internal companion object {
        internal fun Pair<String, JsonNode>.erPart() = try {
            somPart()
            true
        } catch (cause: Throwable) { false }

        internal fun Pair<String, JsonNode>.somPart() = Part(
            identitetsnummer = first,
            fødselsdato = second["fødselsdato"].asLocalDate(),
            navn = second["navn"].asText()
        )
    }
}