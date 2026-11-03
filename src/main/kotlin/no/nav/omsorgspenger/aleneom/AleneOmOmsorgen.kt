package no.nav.omsorgspenger.aleneom

import no.nav.omsorgspenger.BehovssekvensId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import java.time.LocalDate
import java.time.ZonedDateTime

internal data class AleneOmOmsorgen(
    internal val registrert: ZonedDateTime,
    internal val periode: Periode,
    internal val barn: Barn,
    private val behovssekvensId: BehovssekvensId,
    private val regstrertIForbindelseMed: String) {

    internal val kilde = Kilde.internKilde(
        behovssekvensId = behovssekvensId,
        type = regstrertIForbindelseMed
    )

    internal data class Barn(
        internal val identitetsnummer: Identitetsnummer,
        internal val fødselsdato: LocalDate
    )
}

internal data class AleneOmOmsorgenFor(
    internal val identitetsnummer: Identitetsnummer,
    internal val fødselsdato: LocalDate,
    internal val aleneOmOmsorgenI: Periode
)

