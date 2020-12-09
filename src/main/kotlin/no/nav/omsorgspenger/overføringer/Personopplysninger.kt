package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.personopplysninger.Navn
import java.time.LocalDate

internal data class Personopplysninger(
    internal val gjeldendeIdentitetsnummer: Identitetsnummer,
    internal val fødselsdato: LocalDate,
    internal val navn: Navn?,
    internal val aktørId: AktørId,
    internal val adressebeskyttet: Boolean
)