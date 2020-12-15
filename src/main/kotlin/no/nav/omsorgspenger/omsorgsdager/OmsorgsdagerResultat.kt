package no.nav.omsorgspenger.omsorgsdager

internal data class OmsorgsdagerResultat(
    internal val grunnrettsdager: Omsorgsdager,
    internal val aleneomsorgsdager: Omsorgsdager,
    internal val utvidetRettDager: Omsorgsdager,
    internal val aleneomsorgOgUtvidetRettDager: Omsorgsdager) {
    internal val antallOmsorgsdager =
        grunnrettsdager.antallDager +
        aleneomsorgsdager.antallDager +
        utvidetRettDager.antallDager +
        aleneomsorgOgUtvidetRettDager.antallDager

    internal fun kopier(faktor: Int) = copy(
        grunnrettsdager = grunnrettsdager.kopier(faktor),
        aleneomsorgsdager = aleneomsorgsdager.kopier(faktor),
        utvidetRettDager = utvidetRettDager.kopier(faktor),
        aleneomsorgOgUtvidetRettDager = aleneomsorgOgUtvidetRettDager.kopier(faktor)
    )

    internal data class Omsorgsdager(
        internal val antallBarn: Int,
        internal val antallDager: Int) {
        internal fun kopier(faktor: Int) = copy(
            antallDager = antallDager * faktor
        )
    }
}