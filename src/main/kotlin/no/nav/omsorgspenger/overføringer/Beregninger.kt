package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.AntallDager.antallDager
import no.nav.omsorgspenger.lovverk.*
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBeregning.beregnOmsorgsdager

internal object Beregninger {
    private const val DagerMaksForOverføring = 10

    internal fun beregnOmsorgsdagerTilgjengeligForOverføring(
        grunnlag: Grunnlag,
        behandling: Behandling
    ): Map<KnektPeriode, Int> {
        val beregnet = mutableMapOf<KnektPeriode, Int>()
        grunnlag.knekk(behandling.periode).forEach { knektPeriode ->
            beregnet[knektPeriode] = beregnTilgjenegeligeDagerForPeriode(grunnlag, knektPeriode.periode, behandling)
        }
        return beregnet
    }

    private fun beregnTilgjenegeligeDagerForPeriode(grunnlag: Grunnlag, periode: Periode, behandling: Behandling): Int {
        val erMidlertidigAleneIPerioden = grunnlag.midlertidigAleneVedtak.any { it.periode.inneholder(periode) }

        if (erMidlertidigAleneIPerioden) {
            return behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = ErMidlertidigAlenerOmOmsorgen,
                anvendelse = "Har vedtak om midlertidig alene om omsorgen i perioden. Ingen dager kan overføres."
            ).let { 0 }
        }

        val fordelingGirMeldinger = grunnlag.fordelingGirMeldinger.filter { it.periode.inneholder(periode) }
        val omsorgsdagerResultat = beregnOmsorgsdager(
            barnMedOmsorgenFor = grunnlag.overføreOmsorgsdager.barn.filter { it.omsorgenFor.inneholder(periode) }
        )
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

        val antallOmsorgsdager = omsorgsdagerResultat.antallOmsorgsdager

        behandling.lovanvendelser.leggTil(
            periode = periode,
            lovhenvisning = AntallOmsorgsdager,
            anvendelser = setOf(
                "Har $antallOmsorgsdager omsorgsdager",
                "Har grunnrett på ${omsorgsdagerResultat.grunnrettsdager.antallDager} dager"
            )
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

        val dagerTattUtUtoverGrunnretten = when (dagerTattUt > grunnrettsdager.antallDager) {
            true -> dagerTattUt.minus(grunnrettsdager.antallDager).also {
                behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = AlleredeForbrukteDager,
                    anvendelse = "Har tatt ut $it dager utover grunnretten i ${periode.tom.year}"
                )
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

        val tilgjengelig = antallOmsorgsdager - grunnrettsdager.antallDager - dagerTattUtUtoverGrunnretten - fordeltBort

        val tilgjengeligDagerForOverføring = when {
            tilgjengelig > DagerMaksForOverføring -> DagerMaksForOverføring
            tilgjengelig < 0 -> 0
            else -> tilgjengelig
        }

        behandling.lovanvendelser.leggTil(
            periode = periode,
            lovhenvisning = OverføreMaks10Dager,
            anvendelse = "Har $tilgjengeligDagerForOverføring omsorgsdager tilgjengelig for overføring"
        )

        return tilgjengeligDagerForOverføring
    }
}