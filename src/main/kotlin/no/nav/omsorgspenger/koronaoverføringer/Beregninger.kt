package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.AntallDager.antallDager
import no.nav.omsorgspenger.lovverk.*
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBeregning.beregnOmsorgsdager
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBeregning.leggTilLovanvendelser
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import org.slf4j.LoggerFactory

internal object Beregninger {
    private val logger = LoggerFactory.getLogger(Beregninger::class.java)
    private const val KoronaFaktor = 2

    internal fun beregnDagerTilgjengeligForOverføring(
        behandling: Behandling,
        grunnlag: Grunnlag) : Int {

        val omsorgsdagerResultat = beregnOmsorgsdager(
            barnMedOmsorgenFor = grunnlag.behovet.barn.filter { it.omsorgenFor.overlapperMedMinstEnDag(behandling.periode) },
        )

        omsorgsdagerResultat.leggTilLovanvendelser(
            lovanvendelser = behandling.lovanvendelser,
            periode = behandling.periode
        )

        val antallOmsorgsdagerFørDobling = omsorgsdagerResultat.antallOmsorgsdager
        val antallOmsorgsdagerEtterDobling = omsorgsdagerResultat.kopier(faktor = KoronaFaktor).antallOmsorgsdager

        val antallDagerFordelt = grunnlag.antallDagerFordelt(behandling.periode)

        if (antallDagerFordelt > 0) {
            behandling.lovanvendelser.leggTil(
                periode = behandling.periode,
                lovhenvisning = FordeltBortOmsorgsdager,
                anvendelse = "Har fordelt $antallDagerFordelt dager til andre personer"
            )
        }

        val antallDagerOverført = grunnlag.antallDagerOverført(behandling.periode)

        if (antallDagerOverført > 0) {
            behandling.lovanvendelser.leggTil(
                periode = behandling.periode,
                lovhenvisning = OverførtBortOmsorgsdager,
                anvendelse = "Har overført $antallDagerOverført dager til samboer/ektefelle"
            )
        }

        val antallDagerKoronaoverført = grunnlag.koronaoverføringer
            .filter { koronaOverføringer -> koronaOverføringer.periode.overlapperMedMinstEnDag(behandling.periode) }
            .sumBy { it.antallDager }

        if (antallDagerKoronaoverført > 0) {
            behandling.lovanvendelser.leggTil(
                periode = behandling.periode,
                lovhenvisning = KoronaOverføreOmsorgsdager,
                anvendelse = "Har overført $antallDagerKoronaoverført dager ifbm. Koronapandemien til andre personer"
            )
        }

        if (grunnlag.behovet.omsorgsdagerTattUtIÅr > 0) {
            behandling.lovanvendelser.leggTil(
                periode = behandling.periode,
                lovhenvisning = AlleredeForbrukteDager,
                anvendelse = "Har allerede tatt ut ${grunnlag.behovet.omsorgsdagerTattUtIÅr} dager i ${behandling.periode.tom.year}"
            )
        }

        val antallDagerTilgjengelig = antallOmsorgsdagerEtterDobling -
            antallDagerFordelt -
            antallDagerOverført -
            antallDagerKoronaoverført -
            grunnlag.behovet.omsorgsdagerTattUtIÅr

        val antallDagerTilgjengeligForOverføring = when (antallDagerTilgjengelig < 0) {
            true -> 0
            else -> antallDagerTilgjengelig
        }

        behandling.lovanvendelser.leggTil(
            periode = behandling.periode,
            lovhenvisning = DoblingAvAntallDagerKorona,
            anvendelse = "Får doblet antall omsorgsdager fra $antallOmsorgsdagerFørDobling til $antallOmsorgsdagerEtterDobling"
        )
        behandling.lovanvendelser.leggTil(
            periode = behandling.periode,
            lovhenvisning = KoronaOverføreOmsorgsdager,
            anvendelse = "Har $antallDagerTilgjengeligForOverføring omsorgsdager tilgjengelig for overføring ifbm. Koronapandemien"
        )

        return antallDagerTilgjengeligForOverføring
    }

    private fun SpleisetOverføringGitt.periode() = Periode(fom = gyldigFraOgMed, tom = gyldigTilOgMed)
    private fun Grunnlag.antallDagerFordelt(periode: Periode) = fordelinger
        .filter { fordelinger -> fordelinger.periode.overlapperMedMinstEnDag(periode) }
        .let { overlappendeFordelinger -> when {
            overlappendeFordelinger.isEmpty() -> 0
            overlappendeFordelinger.size == 1 -> overlappendeFordelinger.first().lengde.antallDager()
            else -> logger.warn("Fant ${overlappendeFordelinger.size} overlappende fordelinger, bruker lengden på den lengste").let {
                overlappendeFordelinger.maxByOrNull { it.lengde }!!.lengde.antallDager()
            }
        }}
    private fun Grunnlag.antallDagerOverført(periode: Periode) = overføringer
        .filter { overføringer -> overføringer.periode().overlapperMedMinstEnDag(periode) }
        .let { overlappendeOverføringer -> when {
            overlappendeOverføringer.isEmpty() -> 0
            overlappendeOverføringer.size == 1 -> overlappendeOverføringer.first().lengde.antallDager()
            else -> logger.warn("Fant ${overlappendeOverføringer.size} overlappende overføringer, bruker lengde på den lengste").let {
                overlappendeOverføringer.maxByOrNull { it.lengde }!!.lengde.antallDager()
            }
        }}
}

private object DoblingAvAntallDagerKorona : Lovhenvisning {
    override val lov = MidlertidigForskriftIfbmCovid19
    override val henvisning = "§ 4-3"
}

