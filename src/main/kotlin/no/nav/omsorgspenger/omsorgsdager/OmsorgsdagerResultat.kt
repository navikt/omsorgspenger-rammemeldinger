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

    internal data class Omsorgsdager(
        val antallBarn: Int,
        val antallDager: Int
    )
}