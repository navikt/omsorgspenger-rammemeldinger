package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak

internal object Vurderinger {

    internal fun vurderInngangsvilkår(
        grunnlag: Grunnlag,
        context: Context) {
        val overordnetPeriode = grunnlag.overføreOmsorgsdager.overordnetPeriode

        if (!grunnlag.overføreOmsorgsdager.barn.any { it.aleneOmOmsorgen }) {
            context.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = AleneOmOmsorgenForBarnet,
                anvendelse = "Må være alene om omsorgen for mist ett barn for å kunne overføre omsorgsdager."
            )
            context.leggTilKarakteristikk(Context.Karakteristikk.OppfyllerIkkeInngangsvilkår)
        }

        if (!grunnlag.overføreOmsorgsdager.borINorge) {
            context.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = MedlemIFolketrygden,
                anvendelse = "Må være bosatt i Norge for å overføre omsorgsdager."
            )
            context.leggTilKarakteristikk(Context.Karakteristikk.OppfyllerIkkeInngangsvilkår)
        }

        if (!grunnlag.overføreOmsorgsdager.jobberINorge) {
            context.lovanvendelser.leggTil(
                periode = overordnetPeriode,
                lovhenvisning = MedlemIFolketrygden,
                anvendelse = "Må jobbe i Norge for å overføre omsorgsdager."
            )
            context.leggTilKarakteristikk(Context.Karakteristikk.OppfyllerIkkeInngangsvilkår)
        }

        // TODO : samboer minst 1 år..

        if (grunnlag.overføreOmsorgsdager.sendtPerBrev) {
            context.leggTilKarakteristikk(Context.Karakteristikk.MåBesvaresPerBrev)
        }
    }

    internal fun vurderOmsorgenFor(
        grunnlag: Grunnlag,
        context: Context) : Grunnlag {
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
                            context.leggTilKarakteristikk(Context.Karakteristikk.InneholderIkkeVerifiserbareVedtakOmUtvidetRett)
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