package no.nav.omsorgspenger.fordelinger.rivers

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.rivers.HentBehov
import no.nav.omsorgspenger.rivers.LeggTilLøsning
import java.time.ZonedDateTime

internal object FordelingAvOmsorgsdagerMelding : HentBehov<FordelingAvOmsorgsdagerMelding.Behovet>, LeggTilLøsning<FordelingAvOmsorgsdagerMelding.Løsningen> {
    internal const val FordelingAvOmsorgsdager = "FordelingAvOmsorgsdager"

    override fun validateBehov(packet: JsonMessage) {
        packet.requireValue(BehovKeys.Versjon, "1.0.0")
        packet.requireKey(
            BehovKeys.Fra,
            BehovKeys.Til,
            BehovKeys.Mottatt,
            BehovKeys.JournalpostIder
        )
    }

    override fun hentBehov(packet: JsonMessage) = Behovet(
        versjon = packet[BehovKeys.Versjon].asText(),
        fra = packet[BehovKeys.Fra].asText(),
        til = packet[BehovKeys.Til].asText(),
        mottatt = packet[BehovKeys.Mottatt].asText().let { ZonedDateTime.parse(it) },
        barn = packet[BehovKeys.Barn].asText(),
        journalpostIder = (packet[BehovKeys.JournalpostIder] as ArrayNode).map { it.asText() }.toSet().also {
            require(it.isNotEmpty()) { "Må inneholde minst en journalpostId"}
        }
    )

    override fun løsning(løsning: Løsningen): Pair<String, Map<String, *>> = FordelingAvOmsorgsdager to mapOf(
        "utfall" to "GosysJournalføringsoppgaver"
    )

    internal data class Behovet(
        val versjon: String,
        val mottatt: ZonedDateTime,
        val fra:  Identitetsnummer,
        val til: Identitetsnummer,
        val barn: Identitetsnummer,
        val journalpostIder: Set<JournalpostId>
    )

    internal class Løsningen

    private object BehovKeys {
        val Versjon = "@behov.${FordelingAvOmsorgsdager}.versjon"
        val Mottatt = "@behov.${FordelingAvOmsorgsdager}.mottatt"
        val Fra = "@behov.${FordelingAvOmsorgsdager}.fra.identitetsnummer"
        val Til = "@behov.${FordelingAvOmsorgsdager}.til.identitetsnummer"
        val Barn = "@behov.${FordelingAvOmsorgsdager}.barn.identitetsnummer"
        val JournalpostIder = "@behov.${FordelingAvOmsorgsdager}.journalpostIder"
    }
}