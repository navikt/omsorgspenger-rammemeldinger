package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.koronaoverføringer.Perioder.erStøttetPeriode
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import org.slf4j.LoggerFactory

internal object ManuellVurdering {
    private val logger = LoggerFactory.getLogger(ManuellVurdering::class.java)

    internal fun måVurderesManuelt(
        behovet: OverføreKoronaOmsorgsdagerMelding.Behovet
    ) : Boolean {
        val grunnetJobberIkkeINorge = !behovet.jobberINorge.also { if (it) {
            logger.warn("Må vurderes manuelt grunnet at personen ikke jobber i Norge.")
        }}
        val grunnetPeriode = !behovet.periode.erStøttetPeriode().also { if(it) {
            logger.warn("Må vurderes manuelt grunnet at overføringen gjelder for perioden ${behovet.periode} som ikke støttes.")
        }}
        return grunnetJobberIkkeINorge || grunnetPeriode
    }

    internal fun måVurderesManuelt(
        behandling: Behandling,
        grunnlag: Grunnlag,
        dagerTilgjengeligForOverføring: Int) : Boolean {
        val grunnetUtvidetRett = behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett() &&
            dagerTilgjengeligForOverføring < grunnlag.overføringen.omsorgsdagerÅOverføre

        if (grunnetUtvidetRett) {
            logger.warn("Må vurderes manuelt grunnet vedtak om utvidet rett vi ikke kunne verifisere.")
        }
        
        return grunnetUtvidetRett
    }
}