package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Behandling
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.rivers.meldinger.SerDes.JacksonObjectMapper
import no.nav.omsorgspenger.rivers.BehovMedLøsning
import no.nav.omsorgspenger.rivers.HentLøsning

internal object OverføreOmsorgsdagerBehandlingMelding :
    BehovMedLøsning<OverføreOmsorgsdagerBehandlingMelding.HeleBehandling>,
    HentLøsning<OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling> {
    internal const val OverføreOmsorgsdagerBehandling = "OverføreOmsorgsdagerBehandling"

    override fun behovMedLøsning(behovInput: Map<String, *>, løsning: HeleBehandling) =
        Behov(navn = OverføreOmsorgsdagerBehandling, input = behovInput) to mapOf(
            "karakteristikker" to løsning.behandling.karakteristikker().map { it.name },
            "lovanvendelser" to løsning.behandling.lovanvendelser.somLøsning(),
            "overføringer" to JacksonObjectMapper.convertValue(løsning.overføringer),
            "gjeldendeOverføringer" to JacksonObjectMapper.convertValue(løsning.gjeldendeOverføringer),
            "alleSaksnummerMapping" to JacksonObjectMapper.convertValue(løsning.alleSaksnummerMapping),
            "berørteSaksnummer" to løsning.berørteSaksnummer,
            "periode" to løsning.behandling.periode.toString()
        )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(
            LøsningKeys.Karakteristikker,
            LøsningKeys.Overføringer,
            LøsningKeys.GjeldendeOverføringer,
            LøsningKeys.Periode,
            LøsningKeys.AlleSaksnummerMapping,
            LøsningKeys.BerørteSaksnummer
        )
    }

    override fun hentLøsning(packet: JsonMessage) = ForVidereBehandling(
        overføringer = JacksonObjectMapper.readValue(packet[LøsningKeys.Overføringer].toString()),
        gjeldendeOverføringer = JacksonObjectMapper.readValue(packet[LøsningKeys.GjeldendeOverføringer].toString()),
        karakteristikker = JacksonObjectMapper.readValue(packet[LøsningKeys.Karakteristikker].toString()),
        periode = Periode(packet[LøsningKeys.Periode].asText()),
        alleSaksnummerMapping = JacksonObjectMapper.readValue(packet[LøsningKeys.AlleSaksnummerMapping].toString()),
        berørteSaksnummer = packet[LøsningKeys.BerørteSaksnummer].map { it.asText() }.toSet()
    )

    internal data class HeleBehandling(
        internal val behandling: Behandling,
        internal val overføringer: List<NyOverføring>,
        internal val gjeldendeOverføringer: Map<Identitetsnummer, GjeldendeOverføringer>,
        internal val alleSaksnummerMapping: Map<Identitetsnummer, Saksnummer>,
        internal val berørteSaksnummer: Set<Saksnummer>
    )

    internal data class ForVidereBehandling(
        internal val karakteristikker: Set<Behandling.Karakteristikk>,
        internal val overføringer: List<NyOverføring>,
        internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
        internal val alleSaksnummerMapping: Map<Identitetsnummer, Saksnummer>,
        internal val berørteSaksnummer: Set<Saksnummer>,
        internal val periode: Periode) {
        internal val oppfyllerIkkeInngangsvilkår = karakteristikker.contains(Behandling.Karakteristikk.OppfyllerIkkeInngangsvilkår)
        internal val måBesvaresPerBrev = karakteristikker.contains(Behandling.Karakteristikk.MåBesvaresPerBrev)
        internal val ingenOverføringer = overføringer.fjernOverføringerUtenDager().isEmpty()
    }

    private object LøsningKeys {
        const val Karakteristikker = "@løsninger.$OverføreOmsorgsdagerBehandling.karakteristikker"
        const val Overføringer = "@løsninger.$OverføreOmsorgsdagerBehandling.overføringer"
        const val GjeldendeOverføringer = "@løsninger.$OverføreOmsorgsdagerBehandling.gjeldendeOverføringer"
        const val Periode = "@løsninger.$OverføreOmsorgsdagerBehandling.periode"
        const val AlleSaksnummerMapping = "@løsninger.$OverføreOmsorgsdagerBehandling.alleSaksnummerMapping"
        const val BerørteSaksnummer = "@løsninger.$OverføreOmsorgsdagerBehandling.berørteSaksnummer"
    }
}