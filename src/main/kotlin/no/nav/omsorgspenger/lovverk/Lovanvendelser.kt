package no.nav.omsorgspenger.lovverk

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

internal class Lovanvendelser(
    private val lovanvendelser : MutableMap<String, MutableMap<String, MutableList<Lovanvendelse>>> = mutableMapOf()
) {
    internal companion object {
        private val objectMapper = ObjectMapper()
        internal fun fraJson(json: String) = Lovanvendelser(
            lovanvendelser = objectMapper.readValue(json)
        )
    }

    internal fun leggTil(periode: Periode, lovhenvisning: Lovhenvisning, anvendelse: Lovanvendelse) : Lovanvendelser {
        val innenforPeriode = lovanvendelser.getOrDefault(periode.toString(), mutableMapOf())
        val innenforLovhenvisning = innenforPeriode.getOrDefault(lovhenvisning.id(), mutableListOf()).also { it.add(anvendelse) }
        innenforPeriode[lovhenvisning.id()] = innenforLovhenvisning
        lovanvendelser[periode.toString()] = innenforPeriode
        return this
    }

    internal fun somLøsning() = lovanvendelser.toMap()
    internal fun somJson() = objectMapper.writeValueAsString(somLøsning())
    override fun toString(): String = somJson()
    override fun equals(other: Any?): Boolean = other != null && other is Lovanvendelser && other.lovanvendelser == lovanvendelser
}