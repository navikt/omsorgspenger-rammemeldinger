package no.nav.omsorgspenger.omsorgsdager

internal object OmsorgsdagerBeregning {
    private const val DagerMedGrunnrettOppTilToBarn = 10
    private const val DagerMedGrunnrettTreEllerFlerBarn = 15

    private const val DagerMedAleneOmsorgOppTilToBarn = 10
    private const val DagerMedAleneOmsorgTreEllerFlerBarn = 15

    private const val DagerMedUtvidetRettPerBarn = 10
    private const val DagerMedUtvidetRettOgAleneOmsorgPerBarn = 20
    
    internal fun beregnOmsorgsdager(barnMedOmsorgenFor: List<OmsorgsdagerBarn>): OmsorgsdagerResultat {
        val barnMedAleneOmOmsorgen = barnMedOmsorgenFor.filter { it.aleneOmOmsorgen }
        val barnMedKunUtvidetRett = barnMedOmsorgenFor.filter { it.utvidetRett && !it.aleneOmOmsorgen }
        val barnMedUtvidetRettOgAleneOmOmsorgen = barnMedOmsorgenFor.filter { it.utvidetRett && it.aleneOmOmsorgen }

        val grunnrett = when (barnMedOmsorgenFor.size) {
            0 -> OmsorgsdagerResultat.Omsorgsdager(antallBarn = 0, antallDager = 0)
            in 1..2 -> OmsorgsdagerResultat.Omsorgsdager(antallBarn = barnMedOmsorgenFor.size, antallDager = DagerMedGrunnrettOppTilToBarn)
            else -> OmsorgsdagerResultat.Omsorgsdager(antallBarn = barnMedOmsorgenFor.size, antallDager = DagerMedGrunnrettTreEllerFlerBarn)
        }

        val aleneOmsorg = when (barnMedAleneOmOmsorgen.size) {
            0 -> OmsorgsdagerResultat.Omsorgsdager(antallBarn = 0, antallDager = 0)
            in 1..2 -> OmsorgsdagerResultat.Omsorgsdager(antallBarn = barnMedAleneOmOmsorgen.size, antallDager = DagerMedAleneOmsorgOppTilToBarn)
            else -> OmsorgsdagerResultat.Omsorgsdager(antallBarn = barnMedAleneOmOmsorgen.size, antallDager = DagerMedAleneOmsorgTreEllerFlerBarn)
        }

        val utvidetRett = when (barnMedKunUtvidetRett.size) {
            0 -> OmsorgsdagerResultat.Omsorgsdager(antallBarn = 0, antallDager = 0)
            else -> OmsorgsdagerResultat.Omsorgsdager(antallBarn = barnMedKunUtvidetRett.size, antallDager = DagerMedUtvidetRettPerBarn * barnMedKunUtvidetRett.size)
        }

        val aleneomsorgOgUtvidetRett = when (barnMedUtvidetRettOgAleneOmOmsorgen.size) {
            0 -> OmsorgsdagerResultat.Omsorgsdager(antallBarn = 0, antallDager = 0)
            else -> OmsorgsdagerResultat.Omsorgsdager(
                antallBarn = barnMedUtvidetRettOgAleneOmOmsorgen.size,
                antallDager = DagerMedUtvidetRettOgAleneOmsorgPerBarn * barnMedUtvidetRettOgAleneOmOmsorgen.size
            )
        }

        return OmsorgsdagerResultat(
            grunnrettsdager = grunnrett,
            aleneomsorgsdager = aleneOmsorg,
            utvidetRettDager = utvidetRett,
            aleneomsorgOgUtvidetRettDager = aleneomsorgOgUtvidetRett
        )
    }
}