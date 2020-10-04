package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.river.requireArray
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding.Companion.erFordelingGirMelding
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding.Companion.somFordelingGirMelding
import no.nav.omsorgspenger.overføringer.Overføring.Companion.erOverføring
import no.nav.omsorgspenger.overføringer.Overføring.Companion.somOverføring

interface Melding<Innhold> {
    fun validate()
    fun innhold() : Innhold
}

internal object FerdigstillJournalføringForOmsorgspengerMelding {
    internal const val FerdigstillJournalføringForOmsorgspenger = "FerdigstillJournalføringForOmsorgspenger"
    internal fun input(identitetsnummer: Identitetsnummer,
                       journalpostIder: List<String>,
                       saksnummer: String) = mapOf(
        "identitetsnummer" to identitetsnummer,
        "journalpostIder" to journalpostIder,
        "saksnummer" to saksnummer
    )
}

internal class HentFordelingGirMeldingerMelding(private val packet: JsonMessage) : Melding<HentFordelingGirMeldingerMelding.Innhold> {

    override fun validate() {
        packet.interestedIn(Meldinger) { json -> json.requireArray { entry -> entry.erFordelingGirMelding() } }
    }

    override fun innhold() = Innhold(
        meldinger = (packet[Meldinger] as ArrayNode).map { it.somFordelingGirMelding() }
    )

    internal companion object {
        internal const val Navn = "HentFordelingGirMeldinger"
        internal fun input(identitetsnummer: Identitetsnummer, periode: Periode) = mapOf(
            "identitetsnummer" to identitetsnummer,
            "periode" to periode.toString()
        )

        private const val Meldinger = "@løsninger.$Navn.meldinger"
    }

    data class Innhold(
        val meldinger : List<FordelingGirMelding>
    )
}

internal class OverføreOmsorgsdagerBehandlingMelding(private val packet: JsonMessage) : Melding<OverføreOmsorgsdagerBehandlingMelding.Innhold> {
    override fun validate() {
        packet.require(Overføringer) { json -> json.requireArray { entry -> entry.erOverføring() } }
        packet.interestedIn(Karakteristikker)
    }

    override fun innhold() = Innhold(
        overføringer = (packet[Overføringer] as ArrayNode).map { it.somOverføring() },
        karakteristikker = (packet[Karakteristikker] as ArrayNode).map { Behandling.Karakteristikk.valueOf(it.asText()) }.toSet()
    )

    internal companion object {
        internal const val Navn = "OverføreOmsorgsdagerBehandling"
        private const val Overføringer = "@løsninger.$Navn.overføringer"
        private const val Karakteristikker = "@løsninger.$Navn.karakteristikker"
    }

    data class Innhold(
        val overføringer: List<Overføring>,
        val karakteristikker: Set<Behandling.Karakteristikk>
    )
}

internal class OpprettGosysJournalføringsoppgaverMelding(private val packet: JsonMessage) : Melding<Any?> {
    override fun validate() {}
    override fun innhold() = null

    internal companion object {
        internal const val Navn = "OpprettGosysJournalføringsoppgaver"
        private const val Overføringer = "@behov.$Navn.journalpostIder"

        fun input(journalpostIder: List<String>) = mapOf(
            "journalpostIder" to journalpostIder
        )
    }
}
