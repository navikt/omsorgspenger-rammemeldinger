package no.nav.omsorgspenger.koronaoverføringer.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.koronaoverføringer.Behandling
import no.nav.omsorgspenger.koronaoverføringer.NyOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.rivers.BehovMedLøsning
import no.nav.omsorgspenger.rivers.HentLøsning
import no.nav.omsorgspenger.rivers.meldinger.SerDes.JacksonObjectMapper

internal object OverføreKoronaOmsorgsdagerBehandlingMelding :
    BehovMedLøsning<OverføreKoronaOmsorgsdagerBehandlingMelding.HeleBehandling>,
    HentLøsning<OverføreKoronaOmsorgsdagerBehandlingMelding.ForVidereBehandling> {
    internal const val OverføreKoronaOmsorgsdagerBehandling = "OverføreKoronaOmsorgsdagerBehandling"

    internal class HeleBehandling(
        internal val fraSaksnummer: Saksnummer,
        internal val tilSaksnummer: Saksnummer,
        internal val overføringer: List<NyOverføring>,
        internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
        internal val alleSaksnummerMapping: Map<Identitetsnummer, Saksnummer>,
        internal val behandling: Behandling
    )
    internal class ForVidereBehandling(
        internal val fraSaksnummer: Saksnummer,
        internal val tilSaksnummer: Saksnummer,
        internal val overføringer: List<NyOverføring>,
        internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
        internal val alleSaksnummerMapping: Map<Identitetsnummer, Saksnummer>,
        internal val gjennomførtOverføringer: Boolean
    )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(
            LøsningKeys.Overføringer,
            LøsningKeys.GjeldendeOverføringer,
            LøsningKeys.AlleSaksnummerMapping,
            LøsningKeys.FraSaksnummer,
            LøsningKeys.TilSaksnummer,
            LøsningKeys.GjennomførtOverføringer
        )
    }

    override fun hentLøsning(packet: JsonMessage): ForVidereBehandling {
        return ForVidereBehandling(
            fraSaksnummer = packet[LøsningKeys.FraSaksnummer].asText(),
            tilSaksnummer = packet[LøsningKeys.TilSaksnummer].asText(),
            overføringer = (packet[LøsningKeys.Overføringer] as ArrayNode).map { it as ObjectNode }.map { NyOverføring(
                antallDager = it.get("antallDager").asInt(),
                periode = Periode(it.get("periode").asText())
            )},
            alleSaksnummerMapping = JacksonObjectMapper.readValue(packet[LøsningKeys.AlleSaksnummerMapping].toString()),
            gjeldendeOverføringer = JacksonObjectMapper.readValue(packet[LøsningKeys.GjeldendeOverføringer].toString()),
            gjennomførtOverføringer = packet[LøsningKeys.GjennomførtOverføringer].asBoolean()
        )
    }

    override fun behovMedLøsning(behovInput: Map<String, *>, løsning: HeleBehandling): Pair<Behov, Map<String, *>> {
        return Behov(navn = OverføreKoronaOmsorgsdagerBehandling, input = behovInput) to mapOf(
            "overføringer" to løsning.overføringer.map { mapOf(
                "periode" to it.periode.toString(),
                "antallDager" to it.antallDager
            )},
            "fraSaksnummer" to løsning.fraSaksnummer,
            "tilSaksnummer" to løsning.tilSaksnummer,
            "alleSaksnummerMapping" to JacksonObjectMapper.convertValue(løsning.alleSaksnummerMapping),
            "lovanvendelser" to løsning.behandling.lovanvendelser.somLøsning(),
            "inneholderIkkeVerifiserbareVedtakOmUtvidetRett" to løsning.behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett,
            "gjennomførtOverføringer" to løsning.behandling.gjennomførtOverføringer,
            "gjeldendeOverføringer" to JacksonObjectMapper.convertValue(løsning.gjeldendeOverføringer)
        )
    }

    private object LøsningKeys {
        const val Overføringer = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.overføringer"
        const val GjeldendeOverføringer = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.gjeldendeOverføringer"
        const val AlleSaksnummerMapping = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.alleSaksnummerMapping"
        const val FraSaksnummer = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.fraSaksnummer"
        const val TilSaksnummer = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.tilSaksnummer"
        const val GjennomførtOverføringer = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.gjennomførtOverføringer"
    }
}