package no.nav.omsorgspenger.overføringer.gjennomføring

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer

// TODO: Rename Gjennomført -> Gjeldende?
internal data class GjennomførtOverføring(
    val antallDager: Int,
    val periode: Periode,
    val status: Status,
    val type: Type,
    val saksnummerMotpart: Saksnummer) {

    internal enum class Status {
        Aktiv,
        Deaktivert,
        Avslått
    }

    internal enum class Type {
        Fått,
        Gitt
    }
}

internal data class GjennomførteOverføringer(
    val fått: List<GjennomførtOverføring>,
    val gitt: List<GjennomførtOverføring>) {
    init {
        require(fått.none { it.type == GjennomførtOverføring.Type.Gitt })
        require(gitt.none { it.type == GjennomførtOverføring.Type.Fått })
    }
}