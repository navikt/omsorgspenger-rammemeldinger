package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import java.time.Duration
import java.time.LocalDate

internal data class EGjeldendeOverføringer(val gitt: List<EOverføringGitt>, val fått: List<EOverføringFått>)

internal data class EOverføringGitt(
        val gjennomført: LocalDate,
        val periode: Periode,
        val til: AnnenPart,
        val lengde: Duration
)

internal data class EOverføringFått(
        val gjennomført: LocalDate,
        val periode: Periode,
        val fra: AnnenPart,
        val lengde: Duration
)

internal data class AnnenPart(val id: String, val type: String, val fødselsdato: LocalDate)