package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal object OverføreOmsorgsdagerVurderinger {

    internal fun vurderInngangsvilkår(
        grunnlag: OverføreOmsorgsdagerGrunnlag,
        context: OverføreOmsorgsdagerContext) {
        val overordnetPeriode = grunnlag.overføreOmsorgsdager.overordnetPeriode

        if (!grunnlag.overføreOmsorgsdager.barn.any { it.aleneOmOmsorgen }) {
            context.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = AleneOmOmsorgenForBarnet,
                anvendelse = "Må være alene om omsorgen for mist ett barn for å kunne overføre omsorgsdager."
            )
            context.oppfyllerIkkeInngangsvilkår()
        }

        if (!grunnlag.overføreOmsorgsdager.borINorge) {
            context.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = MedlemIFolketrygden,
                anvendelse = "Må være bosatt i Norge for å overføre omsorgsdager."
            )
            context.oppfyllerIkkeInngangsvilkår()
        }

        if (!grunnlag.overføreOmsorgsdager.jobberINorge) {
            context.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = MedlemIFolketrygden,
                anvendelse = "Må jobbe i Norge for å overføre omsorgsdager."
            )
            context.oppfyllerIkkeInngangsvilkår()
        }

        // TODO : samboer minst 1 år..

        if (grunnlag.overføreOmsorgsdager.sendtPerBrev) {
            context.måBesvaresPerBrev()
        }
    }

    internal fun vurderOmsorgenFor(
        grunnlag: OverføreOmsorgsdagerGrunnlag,
        context: OverføreOmsorgsdagerContext) : OverføreOmsorgsdagerGrunnlag {
        val overordnetPeriode = grunnlag.overføreOmsorgsdager.overordnetPeriode
        val utvidetRettVedtak = grunnlag.utvidetRettVedtak.vedtak

        if (!grunnlag.overføreOmsorgsdager.barn.any { it.aleneOmOmsorgen }) {
            context.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = AleneOmOmsorgenForBarnet,
                anvendelse = "Må være alene om omsorgen for mist ett barn for å overføre omsorgsdager."
            )
            return grunnlag
        }

        val barnMedOmsorgenFor = grunnlag
            .overføreOmsorgsdager
            .barn
            .onEach {
                if (it.aleneOmOmsorgen) {
                    context.lovanvendelser.leggTil(
                        periode = overordnetPeriode,
                        lovhenvisning = AleneOmOmsorgenForBarnet,
                        anvendelse = "Er alene om omsorgen for barnet født ${it.fødselsdato}."
                    )
                }
            }
            .filter {
                when (it.utvidetRett) {
                    true -> {
                        val utvidetRettVerifisert = utvidetRettVedtak.inneholderRammevedtakFor(it)
                        if (!utvidetRettVerifisert) {
                            context.inneholderIkkeVerifiserbareOpplysninger()
                        }
                        utvidetRettVerifisert
                    }
                    false -> true
                }
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