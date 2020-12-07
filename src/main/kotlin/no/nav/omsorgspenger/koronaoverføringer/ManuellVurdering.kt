package no.nav.omsorgspenger.koronaoverføringer

internal object ManuellVurdering {
    internal fun måVurderesManuelt(
        behandling: Behandling,
        grunnlag: Grunnlag,
        dagerTilgjengeligForOverføring: Int) : Boolean {
        val grunnetUtvidetRett = behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett() &&
            dagerTilgjengeligForOverføring < grunnlag.overføringen.omsorgsdagerÅOverføre

        return !grunnlag.overføringen.jobberINorge || grunnetUtvidetRett
    }
}