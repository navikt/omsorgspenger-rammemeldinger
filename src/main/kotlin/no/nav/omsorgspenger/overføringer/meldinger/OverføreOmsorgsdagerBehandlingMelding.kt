package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.Behandling
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.Overføring
import no.nav.omsorgspenger.overføringer.meldinger.SerDes.JacksonObjectMapper

internal object OverføreOmsorgsdagerBehandlingMelding :
    BehovMedLøsning<OverføreOmsorgsdagerBehandlingMelding.HeleBehandling>,
    HentLøsning<OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling> {
    internal const val OverføreOmsorgsdagerBehandling = "OverføreOmsorgsdagerBehandling"
    private val behov = Behov(navn = OverføreOmsorgsdagerBehandling)

    override fun behovMedLøsning(løsning: HeleBehandling) =
        behov to mapOf(
            "karakteristikker" to løsning.behandling.karakteristikker().map { it.name },
            "lovanvendelser" to løsning.behandling.lovanvendelser.somLøsning(),
            "overføringer" to JacksonObjectMapper.convertValue(løsning.overføringer),
            "gjeldendeOverføringer" to JacksonObjectMapper.convertValue(løsning.gjeldendeOverføringer),
            "periode" to løsning.behandling.periode.toString()
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(
            LøsningKeys.Karakteristikker,
            LøsningKeys.Overføringer,
            LøsningKeys.GjeldendeOverføringer,
            LøsningKeys.Periode
        )
    }

    override fun hentLøsning(packet: JsonMessage) = ForVidereBehandling(
        overføringer = JacksonObjectMapper.readValue(packet[LøsningKeys.Overføringer].toString()),
        gjeldendeOverføringer = JacksonObjectMapper.readValue(packet[LøsningKeys.GjeldendeOverføringer].toString()),
        karakteristikker = JacksonObjectMapper.readValue(packet[LøsningKeys.Karakteristikker].toString()),
        periode = Periode(packet[LøsningKeys.Periode].asText())
    )

    internal data class HeleBehandling(
        internal val behandling: Behandling,
        internal val overføringer: List<Overføring>,
        internal val gjeldendeOverføringer: Map<Identitetsnummer, GjeldendeOverføringer>
    )

    internal data class ForVidereBehandling(
        internal val karakteristikker: Set<Behandling.Karakteristikk>,
        internal val overføringer: List<Overføring>,
        internal val gjeldendeOverføringer: Map<Identitetsnummer, GjeldendeOverføringer>,
        internal val periode: Periode) {
        internal fun oppfyllerIkkeInngangsvilkår() = karakteristikker.contains(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår)
        internal fun måBesvaresPerBrev() = karakteristikker.contains(Behandling.Karakteristikk.MåBesvaresPerBrev)
    }

    private object LøsningKeys {
        const val Karakteristikker = "@løsninger.$OverføreOmsorgsdagerBehandling.karakteristikker"
        const val Overføringer = "@løsninger.$OverføreOmsorgsdagerBehandling.overføringer"
        const val GjeldendeOverføringer = "@løsninger.$OverføreOmsorgsdagerBehandling.gjeldendeOverføringer"
        const val Periode = "@løsninger.$OverføreOmsorgsdagerBehandling.periode"
    }
}