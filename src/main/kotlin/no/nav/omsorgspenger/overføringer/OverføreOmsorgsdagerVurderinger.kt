package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.lovverk.LovanvendelseBuilder
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal object OverføreOmsorgsdagerVurderinger {

    internal fun vurderInngangsvilkår(
        grunnlag: OverføreOmsorgsdagerGrunnlag,
        lovanvendelseBuilder: LovanvendelseBuilder) {
        //return lovanvendelseBuilder
    }

    internal fun vurderOmsorgenFor(
        grunnlag: OverføreOmsorgsdagerGrunnlag,
        lovanvendelseBuilder: LovanvendelseBuilder) : OverføreOmsorgsdagerGrunnlag {
        val utvidetRettVedtak = grunnlag.utvidetRettVedtak
        val barnMedOmsorgenFor = grunnlag
            .overføreOmsorgsdager
            .barn
            .filter {
                it.aleneOmOmsorgen
            }
            .filter {
                !it.utvidetRett || utvidetRettVedtak.vedtak.inneholderRammevedtakFor(it)
            }

        return grunnlag.copy(
            overføreOmsorgsdager = grunnlag.overføreOmsorgsdager.copy(
                barn = barnMedOmsorgenFor,
            )
        )
    }
}

private fun List<UtvidetRettVedtak>.inneholderRammevedtakFor(barn: Barn) =
    any { it.barnetsFødselsdato.isEqual(barn.fødselsdato) }