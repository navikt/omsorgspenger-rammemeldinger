package no.nav.omsorgspenger.koronaoverføringer.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.koronaoverføringer.NyOverføring
import no.nav.omsorgspenger.rivers.BehovMedLøsning
import no.nav.omsorgspenger.rivers.HentLøsning

internal object OverføreKoronaOmsorgsdagerBehandlingMelding :
    BehovMedLøsning<OverføreKoronaOmsorgsdagerBehandlingMelding.HeleBehandling>,
    HentLøsning<OverføreKoronaOmsorgsdagerBehandlingMelding.ForVidereBehandling> {
    internal const val OverføreKoronaOmsorgsdagerBehandling = "OverføreKoronaOmsorgsdagerBehandling"

    internal class HeleBehandling()
    internal class ForVidereBehandling(
        internal val fraSaksnummer: Saksnummer,
        internal val tilSaksnummer: Saksnummer,
        internal val overføring: NyOverføring
    )

    override fun validateLøsning(packet: JsonMessage) {
        TODO("Not yet implemented")
    }

    override fun hentLøsning(packet: JsonMessage): ForVidereBehandling {
        TODO("Not yet implemented")
    }

    override fun behovMedLøsning(behovInput: Map<String, *>, løsning: HeleBehandling): Pair<Behov, Map<String, *>> {
        TODO("Not yet implemented")
    }
}