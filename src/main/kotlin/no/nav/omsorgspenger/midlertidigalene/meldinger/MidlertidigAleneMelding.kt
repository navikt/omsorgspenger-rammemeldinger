package no.nav.omsorgspenger.midlertidigalene.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.rivers.HentBehov
import no.nav.omsorgspenger.rivers.LeggTilLøsning
import java.time.ZonedDateTime

internal object MidlertidigAleneMelding : HentBehov<MidlertidigAleneMelding.Behovet>, LeggTilLøsning<MidlertidigAleneMelding.Løsningen> {
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
        versjon = packet[BehovKeys.Versjon].asText(),
        søker = packet[BehovKeys.SøkerIdentitetsnummer].asText(),
        annenForelder = packet[BehovKeys.AnnenForelderIdentitetsnummer].asText(),
        mottatt = packet[BehovKeys.Mottatt].asText().let { ZonedDateTime.parse(it) },
        journalpostIder = (packet[BehovKeys.JournalpostIder] as ArrayNode).map { it.asText() }.toSet().also {
            require(it.isNotEmpty()) { "Må inneholde minst en journalpostId"}
        }
    )

    override fun løsning(løsning: Løsningen): Pair<String, Map<String, *>> = MidlertidigAlene to mapOf(
        "utfall" to "GosysJournalføringsoppgaver"
    )

    internal data class Behovet(
        val versjon: String,
        val mottatt: ZonedDateTime,
        val søker:  Identitetsnummer,
        val annenForelder: Identitetsnummer,
        val journalpostIder: Set<JournalpostId>
    )

    internal class Løsningen

    private object BehovKeys {
        val Versjon = "@behov.$MidlertidigAlene.versjon"
        val Mottatt = "@behov.$MidlertidigAlene.mottatt"
        val SøkerIdentitetsnummer = "@behov.$MidlertidigAlene.søker.identitetsnummer"
        val AnnenForelderIdentitetsnummer = "@behov.$MidlertidigAlene.annenForelder.identitetsnummer"
        val JournalpostIder = "@behov.$MidlertidigAlene.journalpostIder"
    }
}