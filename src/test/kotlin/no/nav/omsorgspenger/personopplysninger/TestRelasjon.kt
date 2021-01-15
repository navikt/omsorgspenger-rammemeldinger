package no.nav.omsorgspenger.personopplysninger

import no.nav.omsorgspenger.Identitetsnummer

internal data class TestRelasjon(
    val identitetsnummer: Identitetsnummer = Identitetsnummer(),
    val relasjon: String = "INGEN",
    val borSammen: Boolean = true
)