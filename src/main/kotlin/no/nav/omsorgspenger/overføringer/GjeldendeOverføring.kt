package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer

internal interface GjeldendeOverføring {
    val antallDager: Int
    val periode: Periode
    val status: Status
    enum class Status {
        Aktiv,
        Deaktivert,
        Avslått
    }
}

internal data class GjeldendeOverføringGitt(
    override val antallDager: Int,
    override val periode: Periode,
    override val status: GjeldendeOverføring.Status,
    val til: Saksnummer
) : GjeldendeOverføring

internal data class GjeldendeOverføringFått(
    override val antallDager: Int,
    override val periode: Periode,
    override val status: GjeldendeOverføring.Status,
    val fra: Saksnummer
) : GjeldendeOverføring

internal data class GjeldendeOverføringer(
    val gitt: List<GjeldendeOverføringGitt> = listOf(),
    val fått: List<GjeldendeOverføringFått> = listOf()
)

internal fun Map<Saksnummer, GjeldendeOverføringer>.saksnummer() : Set<Saksnummer> {
    val saksnummer = keys.toMutableSet()
    forEach { _, gjeldendeOverføringer ->
        saksnummer.addAll(gjeldendeOverføringer.fått.map { it.fra })
        saksnummer.addAll(gjeldendeOverføringer.gitt.map { it.til })
    }
    return saksnummer
}