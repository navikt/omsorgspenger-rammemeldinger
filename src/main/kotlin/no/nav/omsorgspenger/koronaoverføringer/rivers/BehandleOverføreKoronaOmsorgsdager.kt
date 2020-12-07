package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.harLøsningPåBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.koronaoverføringer.Behandling
import no.nav.omsorgspenger.koronaoverføringer.Beregninger
import no.nav.omsorgspenger.koronaoverføringer.Grunnlag
import no.nav.omsorgspenger.koronaoverføringer.Grunnlag.Companion.vurdert
import no.nav.omsorgspenger.koronaoverføringer.ManuellVurdering
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentOmsorgspengerSaksnummerMelding
import org.slf4j.LoggerFactory

internal class BehandleOverføreKoronaOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "BehandleOverføreKoronaOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(BehandleOverføreKoronaOmsorgsdager::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreKoronaOmsorgsdagerMelding.OverføreKoronaOmsorgsdager)
                it.harLøsningPåBehov(HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer)
                OverføreKoronaOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val overføringen = OverføreKoronaOmsorgsdagerMelding.hentBehov(packet)

        val saksnummer = HentOmsorgspengerSaksnummerMelding.hentLøsning(packet).also {
            require(it.containsKey(overføringen.fra)) { "Mangler saksnummer for 'fra'"}
            require(it.containsKey(overføringen.til)) { "Mangler saksnummer for 'til'"}
        }

        val behandling = Behandling(overføringen)

        val grunnlag = Grunnlag(
            overføringen = overføringen,
            utvidetRett = listOf(), // TODO
            fordelinger = listOf(), // TODO
            overføringer = listOf(), // TODO
            koronaoverføringer = listOf() // TODO
        ).vurdert(behandling)

        val dagerTilgjengeligForOverføring = Beregninger.beregnDagerTilgjengeligForOverføring(
            behandling = behandling,
            grunnlag = grunnlag
        )

        val måVurderesManuelt = ManuellVurdering.måVurderesManuelt(
            behandling = behandling,
            grunnlag = grunnlag,
            dagerTilgjengeligForOverføring = dagerTilgjengeligForOverføring
        )

        if (måVurderesManuelt) {
            // TODO: Gosysoppgave
        } else {
            // TODO: Gjennomfør overføringen
        }

        return true
    }

}