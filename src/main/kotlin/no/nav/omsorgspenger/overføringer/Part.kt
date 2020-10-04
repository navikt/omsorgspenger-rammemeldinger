package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
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

        internal fun Pair<String, JsonNode>.somPart() : Part {
            val navn = second["navn"] as ObjectNode
            val fornavn = navn["fornavn"].asText()
            val mellomnavn = when (navn.hasNonNull("mellomnavn")) {
                true -> " ${navn["mellomnavn"].asText()} "
                false -> " "
            }
            val etternavn = navn["etternavn"].asText()
            return Part(
                identitetsnummer = first,
                fødselsdato = second["fødselsdato"].asLocalDate(),
                navn = "$fornavn$mellomnavn$etternavn"
            )
        }
    }
}