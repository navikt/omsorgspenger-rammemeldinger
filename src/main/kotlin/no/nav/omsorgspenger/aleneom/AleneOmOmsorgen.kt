package no.nav.omsorgspenger.aleneom

import no.nav.omsorgspenger.AnnenPart
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import java.time.LocalDate

internal data class AleneOmOmsorgen(
        val gjennomført: LocalDate,
        val periode: Periode,
        val barn: AnnenPart,
        val kilder: Set<Kilde>
)