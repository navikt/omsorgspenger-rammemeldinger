package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.AntallDager.antallDager
import no.nav.omsorgspenger.lovverk.*
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBeregning.beregnOmsorgsdager
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBeregning.leggTilLovanvendelser
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerResultat
import no.nav.omsorgspenger.overføringer.apis.periode

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
        val koronaOverføringer = grunnlag.koronaOverføringer.filter { it.periode().inneholder(periode) }
        val omsorgsdagerResultat = beregnOmsorgsdager(
            barnMedOmsorgenFor = grunnlag.overføreOmsorgsdager.barn.filter { it.omsorgenFor.inneholder(periode) }
        )
        val grunnrettsdager = omsorgsdagerResultat.grunnrettsdager
        val antallOmsorgsdager = omsorgsdagerResultat.antallOmsorgsdager

        if (grunnrettsdager.antallBarn == 0) {
            return behandling.lovanvendelser.leggTil(
                periode = periode,
                lovhenvisning = AleneOmOmsorgenForBarnet,
                anvendelse = "Må være alene om omsorgen for minst ett barn for å kunne overføre omsorgsdager."
            ).let { 0 }
        }

        omsorgsdagerResultat.leggTilLovanvendelser(
            lovanvendelser = behandling.lovanvendelser,
            periode = periode
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

        val koronaOverførtTrekkesFra = koronaOverføringer.sumBy { it.lengde.antallDager() }.let { totaltAntallDagerKoronaOverført ->
            if (totaltAntallDagerKoronaOverført <= 0) 0
            else {
                val anvendelser = mutableSetOf(
                    "Har overført $totaltAntallDagerKoronaOverført dager ifbm. Koronapandemien til andre personer"
                )
                val trekkesFra = antallKoronaoverførteDagerSomSkalTrekkesFraTilgjengelig(
                    koronaoverførteDager = totaltAntallDagerKoronaOverført,
                    omsorgsdagerResultat = omsorgsdagerResultat
                )
                if (trekkesFra > 0) anvendelser.add(
                    "$trekkesFra av disse er overført utover ekstradagene ifbm. Koronapandemien og trekkes fra tilgjengelige dager for overføring"
                )

                behandling.lovanvendelser.leggTil(
                    periode = periode,
                    lovhenvisning = KoronaOverføreOmsorgsdager,
                    anvendelser = anvendelser
                )
                trekkesFra
            }
        }

        val tilgjengelig = antallOmsorgsdager - grunnrettsdager.antallDager - dagerTattUtUtoverGrunnretten - fordeltBort - koronaOverførtTrekkesFra

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

    internal fun antallKoronaoverførteDagerSomSkalTrekkesFraTilgjengelig(
        koronaoverførteDager: Int,
        omsorgsdagerResultat: OmsorgsdagerResultat
    ) : Int {
        // Kan koronaoverføre alle ekstradager uten at det påvirker hvor mange dager man ordinært kan overføre.
        val ekstradager = omsorgsdagerResultat.antallOmsorgsdager
        // Kan koronaoverføre alle grunnrettsdager uten at det påvirker hvor mange dager man ordinært kan overføre.
        val grunnrettdager = omsorgsdagerResultat.grunnrettsdager.antallDager
        // Dagene vi sitter igjen med er å anse som dager som kunne vært både ordinært- og korona-overført
        // Derfor trekkes disse fra dagene man har tilgjengelig for ordinær overføring.
        // Dette regnes utifra nåværende grunnlag/omsorgsdagerresultat som ikke nødvendigvis er det samme
        // som da koronaoverførignen ble gjennomført.
        return (koronaoverførteDager - ekstradager - grunnrettdager).let { when (it < 0) {
            true -> 0
            false -> it
        }}
    }
}