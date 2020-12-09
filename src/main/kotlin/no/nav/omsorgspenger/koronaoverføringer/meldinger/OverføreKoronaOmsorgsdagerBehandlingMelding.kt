package no.nav.omsorgspenger.koronaoverføringer.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.koronaoverføringer.NyOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.rivers.BehovMedLøsning
import no.nav.omsorgspenger.rivers.HentLøsning
import no.nav.omsorgspenger.rivers.meldinger.SerDes

internal object OverføreKoronaOmsorgsdagerBehandlingMelding :
    BehovMedLøsning<OverføreKoronaOmsorgsdagerBehandlingMelding.HeleBehandling>,
    HentLøsning<OverføreKoronaOmsorgsdagerBehandlingMelding.ForVidereBehandling> {
    internal const val OverføreKoronaOmsorgsdagerBehandling = "OverføreKoronaOmsorgsdagerBehandling"

    internal class HeleBehandling(
        internal val fraSaksnummer: Saksnummer,
        internal val tilSaksnummer: Saksnummer,
        internal val overføring: NyOverføring,
        internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
        internal val alleSaksnummerMapping: Map<Identitetsnummer, Saksnummer>
    )
    internal class ForVidereBehandling(
        internal val fraSaksnummer: Saksnummer,
        internal val tilSaksnummer: Saksnummer,
        internal val overføring: NyOverføring,
        internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
        internal val alleSaksnummerMapping: Map<Identitetsnummer, Saksnummer>
    )

    override fun validateLøsning(packet: JsonMessage) {
        packet.requireKey(
            LøsningKeys.Overføring,
            LøsningKeys.GjeldendeOverføringer,
            LøsningKeys.AlleSaksnummerMapping,
            LøsningKeys.FraSaksnummer,
            LøsningKeys.TilSaksnummer
        )
    }

    override fun hentLøsning(packet: JsonMessage): ForVidereBehandling {
        return ForVidereBehandling(
            fraSaksnummer = packet[LøsningKeys.FraSaksnummer].asText(),
            tilSaksnummer = packet[LøsningKeys.TilSaksnummer].asText(),
            overføring = (packet[LøsningKeys.Overføring] as ObjectNode).let { NyOverføring(
                antallDager = it.get("antallDager").asInt(),
                periode = Periode(it.get("periode").asText())
            )},
            alleSaksnummerMapping = SerDes.JacksonObjectMapper.readValue(packet[LøsningKeys.AlleSaksnummerMapping].toString()),
            gjeldendeOverføringer = SerDes.JacksonObjectMapper.readValue(packet[LøsningKeys.GjeldendeOverføringer].toString())
        )
    }

    override fun behovMedLøsning(behovInput: Map<String, *>, løsning: HeleBehandling): Pair<Behov, Map<String, *>> {
        return Behov(navn = OverføreKoronaOmsorgsdagerBehandling, input = behovInput) to mapOf(
            "overføring" to mapOf(
                "periode" to løsning.overføring.periode.toString(),
                "antallDager" to løsning.overføring.antallDager
            ),
            "fraSaksnummer" to løsning.fraSaksnummer,
            "tilSaksnummer" to løsning.tilSaksnummer,
            "alleSaksnummerMapping" to løsning.alleSaksnummerMapping,
            "gjeldendeOverføringer" to emptyMap<String,String>() // TODO
        )
    }

    private object LøsningKeys {
        const val Overføring = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.overføring"
        const val GjeldendeOverføringer = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.gjeldendeOverføringer"
        const val AlleSaksnummerMapping = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.alleSaksnummerMapping"
        const val FraSaksnummer = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.fraSaksnummer"
        const val TilSaksnummer = "@løsninger.$OverføreKoronaOmsorgsdagerBehandling.tilSaksnummer"
    }
}