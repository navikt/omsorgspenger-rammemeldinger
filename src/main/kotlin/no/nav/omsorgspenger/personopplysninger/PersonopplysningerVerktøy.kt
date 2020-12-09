package no.nav.omsorgspenger.personopplysninger

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.Identitetsnummer
import java.time.LocalDate

internal object PersonopplysningerVerktøy {
    internal fun ObjectNode.adressebeskyttet() : Boolean {
        require(hasNonNull("adressebeskyttelse"))
        return get("adressebeskyttelse").asText() != "UGRADERT"
    }
    internal fun ObjectNode.navn() : Navn? {
        require(hasNonNull("navn"))
        return if (adressebeskyttet()) null
        else (get("navn") as ObjectNode).let { Navn(
            fornavn = it["fornavn"].asText(),
            mellomnavn = when (it.hasNonNull("mellomnavn")) {
                true -> it["mellomnavn"].asText()
                false -> null
            },
            etternavn = it["etternavn"].asText()
        )
        }
    }
    internal fun ObjectNode.fødselsdato() : LocalDate {
        require(hasNonNull("fødselsdato"))
        return LocalDate.parse(get("fødselsdato").asText())
    }
    internal fun ObjectNode.gjeldendeIdentitetsnummer() : Identitetsnummer {
        require(hasNonNull("gjeldendeIdentitetsnummer"))
        return get("gjeldendeIdentitetsnummer").asText()
    }
    internal fun ObjectNode.aktørId() : AktørId {
        require(hasNonNull("aktørId"))
        return get("aktørId").asText()
    }
}