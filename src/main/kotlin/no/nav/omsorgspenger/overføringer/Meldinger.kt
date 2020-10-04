package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.river.requireArray
import no.nav.omsorgspenger.overføringer.Overføring.Companion.erOverføring
import no.nav.omsorgspenger.overføringer.Overføring.Companion.somOverføring

interface Melding<Innhold> {
    fun validate()
    fun innhold() : Innhold
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