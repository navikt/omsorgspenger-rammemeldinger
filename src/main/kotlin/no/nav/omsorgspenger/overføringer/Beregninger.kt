package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.AntallDager.antallDager

internal object Beregninger {
    private const val DagerMaksForOverføring = 10

    private const val DagerMedGrunnrettOppTilToBarn = 10
    private const val DagerMedGrunnrettTreEllerFlerBarn = 15

    private const val DagerMedAleneOmsorgOppTilToBarn = 10
    private const val DagerMedAleneOmsorgTreEllerFlerBarn = 15

    private const val DagerMedUtvidetRettPerBarn = 10
    private const val DagerMedUtvidetRettOgAleneOmsorgPerBarn = 20

    internal fun beregnOmsorgsdagerTilgjengeligForOverføring(
        grunnlag: Grunnlag,
        behandling: Behandling
    ): Map<KnektPeriode, Int> {
        val beregnet = mutableMapOf<KnektPeriode, Int>()
        grunnlag.knekk(behandling.periode).forEach { knektPeriode ->
            beregnet[knektPeriode] = beregnPeriode(grunnlag, knektPeriode.periode, behandling)
        }
        return beregnet
    }

    private fun beregnPeriode(grunnlag: Grunnlag, periode: Periode, behandling: Behandling): Int {
        val erMidlertidigAleneIPerioden = grunnlag.midlertidigAleneVedtak.any { it.periode.inneholder(periode) }

        if (erMidlertidigAleneIPerioden) {
            return behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = ErMidlertidigAlenerOmOmsorgen,
                anvendelse = "Har vedtak om midlertidig alene om omsorgen i perioden. Ingen dager kan overføres."
            ).let { 0 }
        }

        val fordelingGirMeldinger = grunnlag.fordelingGirMeldinger.filter { it.periode.inneholder(periode) }
        val omsorgsdagerResultat = beregnOmsorgsdager(grunnlag.overføreOmsorgsdager.barn, periode)
        val (
            grunnrettsdager,
            aleneomsorgsdager,
            utvidetRettDager,
            aleneomsorgOgUtvidetRettDager
        ) = omsorgsdagerResultat

        when (grunnrettsdager.antallBarn) {
            0 -> {
                return behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = AleneOmOmsorgenForBarnet,
                    anvendelse = "Må være alene om omsorgen for minst ett barn for å kunne overføre omsorgsdager."
                ).let { 0 }
            }
            in 1..2 -> {
                behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = GrunnrettOppTilToBarn,
                    anvendelse = "Har omsorgen for ${grunnrettsdager.antallBarn} barn"
                )
            }
            else -> {
                behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = GrunnrettTreEllerFlerBarn,
                    anvendelse = "Har omsorgen for ${grunnrettsdager.antallBarn} barn"
                )
            }
        }

        if (aleneomsorgsdager.antallBarn > 0) {
            behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = AleneOmOmsorgenForBarnet,
                anvendelse = "Har aleneomsorg for ${aleneomsorgsdager.antallBarn} barn"
            )
        }

        if (utvidetRettDager.antallBarn > 0) {
            behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = UtvidetRettForBarnet,
                anvendelse = "Har utvidet rett for ${utvidetRettDager.antallBarn} barn"
            )
        }

        if (aleneomsorgOgUtvidetRettDager.antallBarn > 0) {
            behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = UtvidetRettOgAleneOmOmsorgenForBarnet,
                anvendelse = "Har aleneomsorg og utvidet rett for ${aleneomsorgOgUtvidetRettDager.antallBarn} barn"
            )
        }

        val antallOmsorgsdager = omsorgsdagerResultat.antallOmsorgsdager()

        behandling.lovanvendelser.leggTil(
            periode = periode,
            lovhenvisning = AntallOmsorgsdager,
            anvendelse = "Har $antallOmsorgsdager omsorgsdager"
        )

        val dagerTattUt = when (grunnlag.overføreOmsorgsdager.mottaksdato.year == periode.tom.year) {
            true -> grunnlag.overføreOmsorgsdager.omsorgsdagerTattUtIÅr.also {
                if (it > 0) {
                    behandling.lovanvendelser.leggTil(
                        periode = periode,
                        lovhenvisning = AlleredeForbrukteDager,
                        anvendelse = "Har allerede tatt ut $it dager i ${periode.tom.year}"
                    )
                }
            }
            false -> 0
        }

        val fordeltBort = fordelingGirMeldinger.sumBy { it.lengde.antallDager() }.also {
            if (it > 0) {
                behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = FordeltBortOmsorgsdager,
                    anvendelse = "Har fordelt $it dager til andre personer"
                )
            }
        }

        val tilgjengeligeDager = antallOmsorgsdager - dagerTattUt - fordeltBort

        return when (tilgjengeligeDager > DagerMaksForOverføring) {
            true -> DagerMaksForOverføring
            false -> tilgjengeligeDager
        }
    }

    internal fun beregnOmsorgsdager(barn: List<Barn>, periode: Periode): OmsorgsdagerResultat {
        val barnMedOmsorgenFor = barn.filter { it.omsorgenFor.inneholder(periode) }
        val barnMedAleneOmOmsorgen = barnMedOmsorgenFor.filter { it.aleneOmOmsorgen }
        val barnMedKunUtvidetRett = barnMedOmsorgenFor.filter { it.utvidetRett && !it.aleneOmOmsorgen }
        val barnMedUtvidetRettOgAleneOmOmsorgen = barnMedOmsorgenFor.filter { it.utvidetRett && it.aleneOmOmsorgen }

        val grunnrett = when (barnMedOmsorgenFor.size) {
            0 -> DagerForBarn(antallBarn = 0, antallDager = 0)
            in 1..2 -> DagerForBarn(antallBarn = barnMedOmsorgenFor.size, antallDager = DagerMedGrunnrettOppTilToBarn)
            else -> DagerForBarn(antallBarn = barnMedOmsorgenFor.size, antallDager = DagerMedGrunnrettTreEllerFlerBarn)
        }

        val aleneOmsorg = when (barnMedAleneOmOmsorgen.size) {
            0 -> DagerForBarn(antallBarn = 0, antallDager = 0)
            in 1..2 -> DagerForBarn(antallBarn = barnMedAleneOmOmsorgen.size, antallDager = DagerMedAleneOmsorgOppTilToBarn)
            else -> DagerForBarn(antallBarn = barnMedAleneOmOmsorgen.size, antallDager = DagerMedAleneOmsorgTreEllerFlerBarn)
        }

        val utvidetRett = when (barnMedKunUtvidetRett.size) {
            0 -> DagerForBarn(antallBarn = 0, antallDager = 0)
            else -> DagerForBarn(antallBarn = barnMedKunUtvidetRett.size, antallDager = DagerMedUtvidetRettPerBarn * barnMedKunUtvidetRett.size)
        }

        val aleneomsorgOgUtvidetRett = when (barnMedUtvidetRettOgAleneOmOmsorgen.size) {
            0 -> DagerForBarn(antallBarn = 0, antallDager = 0)
            else -> DagerForBarn(
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

data class DagerForBarn(
    val antallBarn: Int,
    val antallDager: Int
)

data class OmsorgsdagerResultat(
    val grunnrettsdager: DagerForBarn,
    val aleneomsorgsdager: DagerForBarn,
    val utvidetRettDager: DagerForBarn,
    val aleneomsorgOgUtvidetRettDager: DagerForBarn,
)

fun OmsorgsdagerResultat.antallOmsorgsdager(): Int =
    grunnrettsdager.antallDager +
        aleneomsorgsdager.antallDager +
        utvidetRettDager.antallDager +
        aleneomsorgOgUtvidetRettDager.antallDager
