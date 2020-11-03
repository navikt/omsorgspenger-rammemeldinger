package no.nav.omsorgspenger.aleneom

import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.AnnenPart
import java.time.Duration
import java.time.LocalDate

internal data class AleneOmOmsorgen(
        val vedtatt: LocalDate,
        val periode: Periode,
        val annenPart: AnnenPart,
        val kilder: Set<Kilde>
)