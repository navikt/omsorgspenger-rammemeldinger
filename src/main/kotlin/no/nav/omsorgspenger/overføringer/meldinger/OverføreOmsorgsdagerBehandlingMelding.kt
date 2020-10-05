package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.Behandling
import no.nav.omsorgspenger.overføringer.Overføring
import no.nav.omsorgspenger.overføringer.Overføring.Companion.somOverføring

internal object OverføreOmsorgsdagerBehandlingMelding :
    BehovMedLøsning<OverføreOmsorgsdagerBehandlingMelding.HeleBehandling>,
    HentLøsning<OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling> {
    internal const val OverføreOmsorgsdagerBehandling = "OverføreOmsorgsdagerBehandling"
    private val behov = Behov(navn = OverføreOmsorgsdagerBehandling)

    override fun behovMedLøsning(løsning: HeleBehandling) =
        behov to mapOf(
            "karakteristikker" to løsning.behandling.karakteristikker().map { it.name },
            "lovanvendelser" to løsning.behandling.lovanvendelser.somLøsning(),
            "overføringer" to løsning.overføringer.map { it.somLøsning() },
            "periode" to løsning.behandling.periode.toString()
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(
            LøsningKeys.Karakteristikker,
            LøsningKeys.Overføringer,
            LøsningKeys.Periode
        )
    }

    override fun hentLøsning(packet: JsonMessage) = ForVidereBehandling(
        overføringer = (packet[LøsningKeys.Overføringer] as ArrayNode).map { it.somOverføring() },
        karakteristikker = (packet[LøsningKeys.Karakteristikker] as ArrayNode).map { Behandling.Karakteristikk.valueOf(it.asText()) }.toSet(),
        periode = Periode(packet[LøsningKeys.Periode].asText())
    )

    internal data class HeleBehandling(
        internal val behandling: Behandling,
        internal val overføringer: List<Overføring>
    )

    internal data class ForVidereBehandling(
        internal val karakteristikker: Set<Behandling.Karakteristikk>,
        internal val overføringer: List<Overføring>,
        internal val periode: Periode
    )

    private object LøsningKeys {
        const val Karakteristikker = "@løsninger.$OverføreOmsorgsdagerBehandling.karakteristikker"
        const val Overføringer = "@løsninger.$OverføreOmsorgsdagerBehandling.overføringer"
        const val Periode = "@løsninger.$OverføreOmsorgsdagerBehandling.periode"
    }
}