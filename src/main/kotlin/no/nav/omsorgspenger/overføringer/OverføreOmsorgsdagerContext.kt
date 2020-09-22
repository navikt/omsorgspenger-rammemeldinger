package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.lovverk.Lovanvendelser

internal class OverføreOmsorgsdagerContext {
    internal val lovanvendelser = Lovanvendelser()

    private val karakteristikker = mutableSetOf<Karakteristikk>()

    internal fun oppfyllerIkkeInngangsvilkår() : OverføreOmsorgsdagerContext {
        karakteristikker.add(Karakteristikk.OppfyllerIkkeInngangsvilkår)
        return this
    }

    internal fun inneholderIkkeVerifiserbareOpplysninger() : OverføreOmsorgsdagerContext {
        karakteristikker.add(Karakteristikk.InneholderIkkeVerifiserbareOpplysninger)
        return this
    }

    internal fun måBesvaresPerBrev() : OverføreOmsorgsdagerContext {
        karakteristikker.add(Karakteristikk.MåBesvaresPerBrev)
        return this
    }

    internal fun karakteristikker() = karakteristikker.toSet()

    internal enum class Karakteristikk {
        OppfyllerIkkeInngangsvilkår,
        InneholderIkkeVerifiserbareOpplysninger,
        MåBesvaresPerBrev
    }
}

