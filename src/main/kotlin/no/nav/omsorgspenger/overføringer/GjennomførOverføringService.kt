package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Saksnummer

internal data class GjennomførtOverføringer(
    internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
    internal val berørteSaksnummer: Set<Saksnummer>
) {
    internal val alleSaksnummer = gjeldendeOverføringer.saksnummer()
    internal fun kunGjeldendeOverføringerForBerørteParter() = GjennomførtOverføringer(
        gjeldendeOverføringer = gjeldendeOverføringer.filterKeys { it in berørteSaksnummer },
        berørteSaksnummer = berørteSaksnummer
    )
}
