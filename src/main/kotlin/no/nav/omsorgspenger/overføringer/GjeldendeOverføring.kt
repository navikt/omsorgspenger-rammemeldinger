package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import java.time.ZonedDateTime

internal interface GjeldendeOverføring {
    val gjennomført: ZonedDateTime
    val antallDager: Int
    val periode: Periode
    val status: Status
    val kilder: Set<Kilde>
    val lovanvendelser: Lovanvendelser?
    enum class Status {
        Aktiv,
        Deaktivert,
        Avslått
    }
}

internal data class GjeldendeOverføringGitt(
    override val gjennomført: ZonedDateTime,
    override val antallDager: Int,
    override val periode: Periode,
    override val status: GjeldendeOverføring.Status,
    override val kilder: Set<Kilde> = setOf(),
    override val lovanvendelser: Lovanvendelser? = null,
    val til: Saksnummer
) : GjeldendeOverføring

internal data class GjeldendeOverføringFått(
    override val gjennomført: ZonedDateTime,
    override val antallDager: Int,
    override val periode: Periode,
    override val status: GjeldendeOverføring.Status,
    override val kilder: Set<Kilde> = setOf(),
    override val lovanvendelser: Lovanvendelser? = null,
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