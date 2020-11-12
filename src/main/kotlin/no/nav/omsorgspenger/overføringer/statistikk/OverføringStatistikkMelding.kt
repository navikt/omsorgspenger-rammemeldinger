package no.nav.omsorgspenger.overføringer.statistikk

import java.time.LocalDate

data class OverføringStatistikkMelding(
        val saksnummer: String,
        val behandlingId: String,
        val mottaksdato: LocalDate
)