package no.nav.omsorgspenger.koronaoverføringer

internal object ManuellVurdering {
    internal fun måVurderesManuelt(
        behandling: Behandling,
        grunnlag: Grunnlag,
        dagerTilgjengeligForOverføring: Int) =
        behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett() &&
        dagerTilgjengeligForOverføring < grunnlag.overføringen.omsorgsdagerÅOverføre
}