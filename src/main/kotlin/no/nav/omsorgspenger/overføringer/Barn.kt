package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

internal data class Barn(
    internal val identitetsnummer: String,
    internal val fødselsdato: LocalDate,
    internal val aleneOmOmsorgen: Boolean,
    internal val utvidetRett: Boolean
) {
    internal companion object {
        internal fun JsonNode.somBarn() = Barn(
            identitetsnummer = get("identitetsnummer").asText(),
            fødselsdato = LocalDate.parse(get("fødselsdato").asText()),
            aleneOmOmsorgen = get("aleneOmOmsorgen").asBoolean(),
            utvidetRett = get("utvidetRett").asBoolean()
        )
    }
}