package no.nav.omsorgspenger.omsorgsdager

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.lovverk.*
import no.nav.omsorgspenger.lovverk.Lovanvendelser

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

    internal fun OmsorgsdagerResultat.leggTilLovanvendelser(
        lovanvendelser: Lovanvendelser,
        periode: Periode) {

        if (grunnrettsdager.antallBarn in 1..2) {
            lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = GrunnrettOppTilToBarn,
                anvendelse = "Har omsorgen for ${grunnrettsdager.antallBarn} barn"
            )
        }
        if (grunnrettsdager.antallBarn >= 3) {
            lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = GrunnrettTreEllerFlerBarn,
                anvendelse = "Har omsorgen for ${grunnrettsdager.antallBarn} barn"
            )
        }

        if (aleneomsorgsdager.antallBarn > 0) {
            lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = AleneOmOmsorgenForBarnet,
                anvendelse = "Har aleneomsorg for ${aleneomsorgsdager.antallBarn} barn"
            )
        }

        if (utvidetRettDager.antallBarn > 0) {
            lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = UtvidetRettForBarnet,
                anvendelse = "Har utvidet rett for ${utvidetRettDager.antallBarn} barn"
            )
        }

        if (aleneomsorgOgUtvidetRettDager.antallBarn > 0) {
            lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = UtvidetRettOgAleneOmOmsorgenForBarnet,
                anvendelse = "Har aleneomsorg og utvidet rett for ${aleneomsorgOgUtvidetRettDager.antallBarn} barn"
            )
        }

        lovanvendelser.leggTil(
            periode = periode,
            lovhenvisning = AntallOmsorgsdager,
            anvendelser = setOf(
                "Har $antallOmsorgsdager omsorgsdager",
                "Har grunnrett på ${grunnrettsdager.antallDager} dager"
            )
        )
    }
}