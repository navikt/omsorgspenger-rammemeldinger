package no.nav.omsorgspenger.midlertidigalene.rivers

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.rivers.HentBehov
import java.time.ZonedDateTime

internal object MidlertidigAleneMelding : HentBehov<MidlertidigAleneMelding.Behovet> {
    internal const val MidlertidigAlene = "MidlertidigAlene"

    override fun validateBehov(packet: JsonMessage) {
        packet.requireValue(BehovKeys.Versjon, "1.0.0")
        packet.requireKey(
            BehovKeys.SøkerIdentitetsnummer,
            BehovKeys.AnnenForelderIdentitetsnummer,
            BehovKeys.Mottatt,
            BehovKeys.JournalpostIder
        )
    }

    override fun hentBehov(packet: JsonMessage) = Behovet(
        søker = packet[BehovKeys.SøkerIdentitetsnummer].asText(),
        annenForelder = packet[BehovKeys.AnnenForelderIdentitetsnummer].asText(),
        mottatt = packet[BehovKeys.Mottatt].asText().let { ZonedDateTime.parse(it) },
        journalpostIder = (packet[BehovKeys.JournalpostIder] as ArrayNode).map { it.asText() }.toSet().also {
            require(it.isNotEmpty()) { "Må inneholde minst en journalpostId"}
        }
    )

    internal data class Behovet(
        val mottatt: ZonedDateTime,
        val søker:  Identitetsnummer,
        val annenForelder: Identitetsnummer,
        val journalpostIder: Set<JournalpostId>
    )

    private object BehovKeys {
        val Versjon = "@behov.${MidlertidigAlene}.versjon"
        val Mottatt = "@behov.${MidlertidigAlene}.mottatt"
        val SøkerIdentitetsnummer = "@behov.${MidlertidigAlene}.søker.identitetsnummer"
        val AnnenForelderIdentitetsnummer = "@behov.${MidlertidigAlene}.annenForelder.identitetsnummer"
        val JournalpostIder = "@behov.${MidlertidigAlene}.journalpostIder"
    }
}