package no.nav.omsorgspenger.lovverk

import no.nav.omsorgspenger.Periode

interface Lov {
    val id: String
}

interface Lovhenvisning {
    val lov : Lov
    val henvisning: String
    fun id() = "${lov.id} $henvisning"
}

typealias Lovanvendelse = String

internal class Lovanvendelser {
    private val lovanvendelser : MutableMap<String, MutableMap<String, MutableList<Lovanvendelse>>> = mutableMapOf()

    internal fun leggTil(periode: Periode, lovhenvisning: Lovhenvisning, anvendelse: Lovanvendelse) : Lovanvendelser {
        val innenforPeriode = lovanvendelser.getOrDefault(periode.toString(), mutableMapOf())
        val innenforLovhenvisning = innenforPeriode.getOrDefault(lovhenvisning.id(), mutableListOf()).also { it.add(anvendelse) }
        innenforPeriode[lovhenvisning.id()] = innenforLovhenvisning
        lovanvendelser[periode.toString()] = innenforPeriode
        return this
    }

    internal fun somLøsning() = lovanvendelser.toMap()
}