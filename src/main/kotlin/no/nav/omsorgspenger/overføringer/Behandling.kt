package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.lovverk.Lovanvendelser

internal class Behandling {

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

    internal fun somLøsning(nyeOverføringer: List<Overføring>) = mapOf(
        "karakteristikker" to karakteristikker.map { it.name },
        "lovanvendelser" to lovanvendelser.somLøsning(),
        "overføringer" to nyeOverføringer.map { it.somLøsning() }
    )

    internal enum class Karakteristikk {
        OppfyllerIkkeInngangsvilkår,
        InneholderIkkeVerifiserbareVedtakOmUtvidetRett,
        MåBesvaresPerBrev
    }
}

