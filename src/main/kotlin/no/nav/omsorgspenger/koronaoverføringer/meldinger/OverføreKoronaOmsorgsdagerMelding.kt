package no.nav.omsorgspenger.koronaoverføringer.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.sisteDagIÅret
import no.nav.omsorgspenger.rivers.HentBehov
import java.time.LocalDate
import java.time.ZonedDateTime

internal object OverføreKoronaOmsorgsdagerMelding : HentBehov<OverføreKoronaOmsorgsdagerMelding.Behovet> {
    internal const val OverføreKoronaOmsorgsdager = "OverføreKoronaOmsorgsdager"

    internal data class Behovet(
        val fra: Identitetsnummer,
        val til: Identitetsnummer,
        val barn: List<Barn>,
        val periode: Periode,
        val mottatt: ZonedDateTime,
        val mottaksdato: LocalDate,
        val omsorgsdagerÅOverføre: Int,
        val omsorgsdagerTattUtIÅr: Int // TODO: Rename ?
    )

    internal data class Løsningen(
        val bar: Boolean
    )

    internal data class Barn(
        internal val identitetsnummer: String,
        internal val fødselsdato: LocalDate,
        internal val aleneOmOmsorgen: Boolean,
        internal val utvidetRett: Boolean) {
        internal val omsorgenFor = Periode(
            fom = fødselsdato,
            tom = fødselsdato
                .plusYears(when (utvidetRett) {
                    true -> 18L
                    false -> 12L
                })
                .sisteDagIÅret()
        )
    }

    override fun validateBehov(packet: JsonMessage) {
        TODO("Not yet implemented")
    }

    override fun hentBehov(packet: JsonMessage): Behovet {
        TODO("Not yet implemented")
    }
}