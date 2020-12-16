package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.AntallDager.antallDager
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBeregning.beregnOmsorgsdager
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
        ).kopier(faktor = KoronaFaktor)

        val antallDagerFordelt = grunnlag.fordelinger
            .filter { fordelinger -> fordelinger.periode.overlapperMedMinstEnDag(behandling.periode) }
            .let { overlappendeFordelinger -> when {
                overlappendeFordelinger.isEmpty() -> 0
                overlappendeFordelinger.size == 1 -> overlappendeFordelinger.first().lengde.antallDager()
                else -> logger.warn("Fant ${overlappendeFordelinger.size} overlappende fordelinger, bruker lengden på den lengste").let {
                    overlappendeFordelinger.maxByOrNull { it.lengde }!!.lengde.antallDager()
                }
            }}

        val antallDagerOverført = grunnlag.overføringer
            .filter { overføringer -> overføringer.periode().overlapperMedMinstEnDag(behandling.periode) }
            .let { overlappendeOverføringer -> when {
                overlappendeOverføringer.isEmpty() -> 0
                overlappendeOverføringer.size == 1 -> overlappendeOverføringer.first().lengde.antallDager()
                else -> logger.warn("Fant ${overlappendeOverføringer.size} overlappende overføringer, bruker lengde på den lengste").let {
                    overlappendeOverføringer.maxByOrNull { it.lengde }!!.lengde.antallDager()
                }
            }}

        val antallDagerKoronaoverført = grunnlag.koronaoverføringer
            .filter { koronaOverføringer -> koronaOverføringer.periode.overlapperMedMinstEnDag(behandling.periode) }
            .sumBy { it.antallDager }

        val antallDagerTilgjengelig = omsorgsdagerResultat.antallOmsorgsdager -
            antallDagerFordelt -
            antallDagerOverført -
            antallDagerKoronaoverført -
            grunnlag.behovet.omsorgsdagerTattUtIÅr

        val antallDagerTilgjengeligForOverføring = when (antallDagerTilgjengelig < 0) {
            true -> 0
            else -> antallDagerTilgjengelig
        }

        return antallDagerTilgjengeligForOverføring
    }

    private fun SpleisetOverføringGitt.periode() = Periode(fom = gyldigFraOgMed, tom = gyldigTilOgMed)
}

