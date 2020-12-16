package no.nav.omsorgspenger.koronaoverføringer

import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtakVurderinger.inneholderRammevedtakFor

internal data class Grunnlag(
    internal val behovet: OverføreKoronaOmsorgsdagerMelding.Behovet,
    internal val utvidetRett: List<UtvidetRettVedtak>,
    internal val fordelinger: List<FordelingGirMelding>,
    internal val overføringer: List<SpleisetOverføringGitt>,
    internal val koronaoverføringer : List<GjeldendeOverføringGitt>) {
    internal companion object {
        internal fun Grunnlag.vurdert(behandling: Behandling) : Grunnlag {

            val barnMedUtvidetRettSomIkkeKanVerifiseres = behovet.barn
                .filter { it.utvidetRett && !utvidetRett.inneholderRammevedtakFor(
                    barnetsFødselsdato = it.fødselsdato,
                    omsorgenForBarnet = it.omsorgenFor,
                    mottaksdato = behovet.mottaksdato
                )}.also { if (it.isNotEmpty()) {
                    behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett = true
                }}

            return copy(
                behovet = behovet.copy(
                    barn = behovet.barn
                        .minus(barnMedUtvidetRettSomIkkeKanVerifiseres)
                        .plus(barnMedUtvidetRettSomIkkeKanVerifiseres.map { it.copy(utvidetRett = false) })
                )
            )
        }
    }
}