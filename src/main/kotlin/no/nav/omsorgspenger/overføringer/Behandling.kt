package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.lovverk.Lovanvendelser

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

    internal fun avslag() = karakteristikker.contains(Karakteristikk.OppfyllerIkkeInngangsvilkår)

    internal enum class Karakteristikk {
        OppfyllerIkkeInngangsvilkår,
        InneholderIkkeVerifiserbareVedtakOmUtvidetRett,
        MåBesvaresPerBrev
    }
}