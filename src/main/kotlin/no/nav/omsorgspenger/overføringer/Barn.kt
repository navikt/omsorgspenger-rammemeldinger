package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.sisteDagIÅret
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBarn
import java.time.LocalDate

internal data class Barn(
    internal val identitetsnummer: String,
    internal val fødselsdato: LocalDate,
    override val aleneOmOmsorgen: Boolean,
    override val utvidetRett: Boolean) : OmsorgsdagerBarn {

    private val omsorgenForBarnetUtÅretBarnetFyller = when (utvidetRett) {
        true -> 18
        false -> 12
    }

    internal val omsorgenFor = Periode(
        fom = fødselsdato,
        tom = fødselsdato
            .plusYears(omsorgenForBarnetUtÅretBarnetFyller.toLong())
            .sisteDagIÅret()
    )

    internal companion object {
        internal fun JsonNode.somBarn() = Barn(
            identitetsnummer = get("identitetsnummer").asText(),
            fødselsdato = LocalDate.parse(get("fødselsdato").asText()),
            aleneOmOmsorgen = get("aleneOmOmsorgen").asBoolean(),
            utvidetRett = get("utvidetRett").asBoolean()
        )

        internal fun List<Barn>.sisteDatoMedOmsorgenForOgAleneOmOmsorgen() =
            filter { it.aleneOmOmsorgen }.maxByOrNull { it.omsorgenFor.tom }?.let { barn ->
                barn.omsorgenFor.tom to barn.utvidetRett
            }
    }
}
