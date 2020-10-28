package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.Identitetsnummer
import java.time.LocalDate

internal data class Personopplysninger(
    internal val identitetsnummer: Identitetsnummer,
    internal val fødselsdato: LocalDate,
    internal val navn: Navn,
    internal val aktørId: AktørId) {
    internal companion object {
        internal fun Pair<String, JsonNode>.somPersonopplysninger() : Personopplysninger {
            val navn = second["navn"] as ObjectNode
            val fornavn = navn["fornavn"].asText()
            val mellomnavn = when (navn.hasNonNull("mellomnavn")) {
                true -> " ${navn["mellomnavn"].asText()} "
                false -> null
            }
            val etternavn = navn["etternavn"].asText()
            return Personopplysninger(
                identitetsnummer = first,
                fødselsdato = second["fødselsdato"].asLocalDate(),
                navn = Navn(
                    fornavn = fornavn,
                    mellomnavn = mellomnavn,
                    etternavn = etternavn
                ),
                aktørId = second["aktørId"].asText()
            )
        }
    }
    internal data class Navn(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String) {
        override fun toString() = when (mellomnavn) {
            null -> "$fornavn $etternavn"
            else -> "$fornavn $mellomnavn $etternavn"
        }
    }
}