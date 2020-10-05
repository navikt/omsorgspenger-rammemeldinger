package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.lovverk.Lovanvendelser

internal class Behandling(
    sendtPerBrev: Boolean,
    internal val periode: Periode
) {

    init {
        if (sendtPerBrev) {
            leggTilKarakteristikk(Karakteristikk.MåBesvaresPerBrev)
        }
    }

    internal val lovanvendelser = Lovanvendelser()

    private val karakteristikker = mutableSetOf<Karakteristikk>()

    internal fun oppfyllerIkkeInngangsvilkår() = karakteristikker.contains(Karakteristikk.OppfyllerIkkeInngangsvilkår)

    internal fun inneholderIkkeVerifiserbareVedtakOmUtvidetRett() = karakteristikker.contains(Karakteristikk.InneholderIkkeVerifiserbareVedtakOmUtvidetRett)

    internal fun måBesvaresPerBrev() = karakteristikker.contains(Karakteristikk.MåBesvaresPerBrev)

    internal fun leggTilKarakteristikk(karakteristikk: Karakteristikk) : Behandling {
        karakteristikker.add(karakteristikk)
        return this
    }

    internal fun karakteristikker() = karakteristikker.toSet()

    internal enum class Karakteristikk {
        OppfyllerIkkeInngangsvilkår,
        InneholderIkkeVerifiserbareVedtakOmUtvidetRett,
        MåBesvaresPerBrev
    }
}

