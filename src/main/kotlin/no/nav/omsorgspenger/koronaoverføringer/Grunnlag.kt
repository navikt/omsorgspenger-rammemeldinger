package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.lovverk.JobberINorge
import no.nav.omsorgspenger.lovverk.UtvidetRettForBarnet
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtakVurderinger.inneholderRammevedtakFor

internal data class Grunnlag(
    internal val behovet: OverføreKoronaOmsorgsdagerMelding.Behovet,
    internal val utvidetRett: List<UtvidetRettVedtak>,
    internal val fordelinger: List<FordelingGirMelding>,
    internal val overføringer: List<SpleisetOverføringGitt>,
    internal val relasjoner: Set<VurderRelasjonerMelding.Relasjon>,
    internal val koronaoverføringer : List<GjeldendeOverføringGitt>) {
    internal companion object {
        internal fun Grunnlag.vurdert(behandling: Behandling) : Grunnlag {

            behandling.lovanvendelser.leggTil(
                periode = behandling.periode,
                lovhenvisning = JobberINorge,
                anvendelse = when (behovet.jobberINorge) {
                    true -> "Jobber i Norge."
                    false -> throw IllegalStateException("Skal ikke havne inn til behandling.")
                }
            )

            val barnMedUtvidetRettSomIkkeKanVerifiseres = behovet.barn
                .filter { it.utvidetRett && !utvidetRett.inneholderRammevedtakFor(
                    barnetsFødselsdato = it.fødselsdato,
                    omsorgenForBarnet = it.omsorgenFor,
                    mottaksdato = behovet.mottaksdato
                )}.also { if (it.isNotEmpty()) {
                    behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett = true
                }}.onEach {
                    behandling.lovanvendelser.leggTil(
                        periode = behandling.periode,
                        lovhenvisning = UtvidetRettForBarnet,
                        anvendelse = "Kunne ikke verifiser utvidet rett for barnet født ${it.fødselsdato}"
                    )
                }

            val barnSomBorNånAnnenstans = behovet.barn
                .filter { barnMedUtvidetRettSomIkkeKanVerifiseres.contains(it) }
                .filterNot { barn ->
                    relasjoner.first { it.identitetsnummer == barn.identitetsnummer }.borSammen
                }.onEach {
                    behandling.lovanvendelser.leggTil(
                        periode = behandling.periode,
                        lovhenvisning = UtvidetRettForBarnet, // TODO: Var står det att man måste bo med barnet?
                        anvendelse = "Bor ikke sammen med barnet født ${it.fødselsdato}"
                    )
                }

            return copy(
                behovet = behovet.copy(
                    barn = behovet.barn
                        .minus(barnMedUtvidetRettSomIkkeKanVerifiseres)
                        .plus(barnMedUtvidetRettSomIkkeKanVerifiseres.map { it.copy(utvidetRett = false) })
                        .minus(barnSomBorNånAnnenstans)
                        .plus(barnSomBorNånAnnenstans.map { it.copy(utvidetRett = false)})
                )
            )
        }
    }
}