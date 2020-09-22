package no.nav.omsorgspenger.lovverk

import no.nav.omsorgspenger.Periode

interface Lov {
    val navn: String
    val versjon: String
}

interface Lovhenvisning {
    val lov : Lov
    val henvisning: String
}

typealias Lovanvendelse = String

internal typealias Lovanvendelser = Map<Periode, Map<Lovhenvisning, List<Lovanvendelse>>>

internal class LovanvendelseBuilder {
    private val lovanvendelser : MutableMap<Periode, MutableMap<Lovhenvisning, MutableList<Lovanvendelser>>> = mutableMapOf()

    internal fun leggTil(periode: Periode, lovhenvisning: Lovhenvisning, anvendele: Lovanvendelse) {
        lovanvendelser.replace(periode, lovanvendelser.getOrDefault(periode, mutableMapOf()).put())
    }

    internal fun build() = lovanvendelser.toMap()
}