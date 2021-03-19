package no.nav.omsorgspenger.aleneom.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.rivers.HentBehov
import java.time.LocalDate

internal object AleneOmOmsorgenMelding : HentBehov<AleneOmOmsorgenMelding.Behovet> {
    internal const val AleneOmOmsorgen = "AleneOmOmsorgen"

    override fun validateBehov(packet: JsonMessage) {
        packet.requireValue(BehovKeys.Versjon, "1.1.0")
        packet.interestedIn(
            BehovKeys.Mottaksdato,
            BehovKeys.Identitetsnumer,
            BehovKeys.Barn
        )
    }

    override fun hentBehov(packet: JsonMessage): Behovet {
        return Behovet(
            identitetsnummer = packet[BehovKeys.Identitetsnumer].asText(),
            mottaksdato = packet[BehovKeys.Mottaksdato].asLocalDate(),
            barn = (packet[BehovKeys.Barn] as ArrayNode).map { barn -> Barn(
                identitetsnummer = barn["identitetsnummer"].asText()
            )}
        )
    }

    internal data class Barn(
        internal val identitetsnummer: Identitetsnummer,
    )

    internal data class Behovet(
        val identitetsnummer: Identitetsnummer,
        val mottaksdato: LocalDate,
        val barn: List<Barn>
    )

    private object BehovKeys {
        val Versjon = "@behov.${AleneOmOmsorgen}.versjon"
        val Mottaksdato = "@behov.${AleneOmOmsorgen}.mottaksdato"
        val Identitetsnumer = "@behov.${AleneOmOmsorgen}.identitetsnummer"
        val Barn = "@behov.${AleneOmOmsorgen}.barn"
    }
}