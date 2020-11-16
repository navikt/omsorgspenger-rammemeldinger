package no.nav.omsorgspenger.overføringer.statistikk

import java.time.LocalDate
import java.time.OffsetDateTime

data class OverføringStatistikkMelding(
        val saksnummer: String,
        val behandlingId: String,
        val mottaksdato: LocalDate,
        val registrertDato: LocalDate,
        val behandlingType: String,
        val behandlingStatus: String,
        val funksjonellTid: OffsetDateTime,
        val aktørId: String
)