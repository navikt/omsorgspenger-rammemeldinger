package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import no.nav.omsorgspenger.overføringer.Behandling.Karakteristikk.Companion.avslag

internal class Behandling(
    sendtPerBrev: Boolean,
    internal val periode: Periode) {
    private val karakteristikker = mutableSetOf<Karakteristikk>().also {
        if (sendtPerBrev) {
            it.add(Karakteristikk.MåBesvaresPerBrev)
        }
    }

    internal val lovanvendelser = Lovanvendelser()

    internal fun inneholderIkkeVerifiserbareVedtakOmUtvidetRett() = karakteristikker.contains(Karakteristikk.InneholderIkkeVerifiserbareVedtakOmUtvidetRett)

    internal fun leggTilKarakteristikk(karakteristikk: Karakteristikk) : Behandling {
        karakteristikker.add(karakteristikk)
        return this
    }

    internal fun karakteristikker() = karakteristikker.toSet()

    internal fun avslag() = karakteristikker.avslag()

    internal enum class Karakteristikk {
        OppfyllerIkkeInngangsvilkår,
        IkkeOmsorgenForNoenBarn,
        IkkeSammeAdresseSomMottaker,
        InneholderIkkeVerifiserbareVedtakOmUtvidetRett,
        MåBesvaresPerBrev;

        internal companion object {
            private val AvslagsKarakteristikker = setOf(
                OppfyllerIkkeInngangsvilkår,
                IkkeOmsorgenForNoenBarn,
                IkkeSammeAdresseSomMottaker
            )
            internal fun Set<Karakteristikk>.avslag() = AvslagsKarakteristikker.intersect(this).isNotEmpty()
        }

    }
}