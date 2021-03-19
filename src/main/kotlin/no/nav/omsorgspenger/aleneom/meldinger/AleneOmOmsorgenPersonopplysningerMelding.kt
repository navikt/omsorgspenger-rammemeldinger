package no.nav.omsorgspenger.aleneom.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.personopplysninger.PersonopplysningerVerktøy.fødselsdato
import java.time.LocalDate

internal object AleneOmOmsorgenPersonopplysningerMelding
    : HentPersonopplysningerMelding<LocalDate, Any>(
    defaultAttributter = setOf("fødselsdato"), måFinneAllePersoner = true) {
    internal val HentPersonopplysninger = HentPersonopplysningerMelding.HentPersonopplysninger
    override fun mapFellesopplysninger(input: JsonNode) = Any()
    override fun mapPersonopplysninger(input: Map<Identitetsnummer, ObjectNode>): Map<Identitetsnummer, LocalDate> {
        return input.mapValues { (_,json) -> json.fødselsdato() }
    }
}