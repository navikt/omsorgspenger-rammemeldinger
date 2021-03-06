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

    internal fun leggTil(periode: Periode, lovhenvisning: Lovhenvisning, anvendelser: Set<Lovanvendelse>) : Lovanvendelser {
        val innenforPeriode = lovanvendelser.getOrDefault(periode.toString(), mutableMapOf())
        val innenforLovhenvisning = innenforPeriode.getOrDefault(lovhenvisning.id(), mutableListOf()).also { it.addAll(anvendelser) }
        innenforPeriode[lovhenvisning.id()] = innenforLovhenvisning
        lovanvendelser[periode.toString()] = innenforPeriode
        return this
    }

    internal fun leggTil(periode: Periode, lovhenvisning: Lovhenvisning, anvendelse: Lovanvendelse) =
        leggTil(periode, lovhenvisning, setOf(anvendelse))

    internal fun kunAnvendelser() : Map<Periode, List<Lovanvendelse>> {
        return lovanvendelser.map { (periode,lovanvendelser) ->
            Periode(periode) to lovanvendelser.values.flatten()
        }.toMap()
    }
    internal fun somLøsning() = lovanvendelser.toMap()
    internal fun somJson() = objectMapper.writeValueAsString(somLøsning())
    override fun toString(): String = somJson()
    override fun equals(other: Any?): Boolean = other != null && other is Lovanvendelser && other.lovanvendelser == lovanvendelser
}