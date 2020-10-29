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
    internal val navn: Navn?,
    internal val aktørId: AktørId) {
    internal companion object {
        internal fun Pair<String, JsonNode>.somPersonopplysninger() : Personopplysninger {
            val adressebeskyttet = second["adressebeskyttelse"]?.asText() != "UGRADERT"
            val navn = when (adressebeskyttet) {
                true -> null
                false -> (second["navn"] as ObjectNode).let { Navn(
                    fornavn = it["fornavn"].asText(),
                    mellomnavn = when (it.hasNonNull("mellomnavn")) {
                        true -> it["mellomnavn"].asText()
                        false -> null
                    },
                    etternavn = it["etternavn"].asText()
                )}
            }
            return Personopplysninger(
                identitetsnummer = first,
                fødselsdato = second["fødselsdato"].asLocalDate(),
                navn = navn,
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