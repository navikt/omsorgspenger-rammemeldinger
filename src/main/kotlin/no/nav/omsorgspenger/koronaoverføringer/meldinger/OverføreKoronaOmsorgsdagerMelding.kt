package no.nav.omsorgspenger.koronaoverføringer.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.sisteDagIÅret
import no.nav.omsorgspenger.extensions.toLocalDateOslo
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBarn
import no.nav.omsorgspenger.rivers.HentBehov
import no.nav.omsorgspenger.rivers.LeggTilLøsning
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding
import java.time.LocalDate
import java.time.ZonedDateTime

internal object OverføreKoronaOmsorgsdagerMelding :
    HentBehov<OverføreKoronaOmsorgsdagerMelding.Behovet>,
    LeggTilLøsning<OverføreKoronaOmsorgsdagerMelding.Løsningen> {
    internal const val OverføreKoronaOmsorgsdager = "OverføreKoronaOmsorgsdager"

    internal data class Behovet(
        val fra: Identitetsnummer,
        val til: Identitetsnummer,
        val jobberINorge: Boolean,
        val barn: List<Barn>,
        val periode: Periode,
        val mottatt: ZonedDateTime,
        val mottaksdato: LocalDate,
        val journalpostIder: Set<JournalpostId>,
        val omsorgsdagerÅOverføre: Int,
        val omsorgsdagerTattUtIÅr: Int) {
        internal fun somOpprettGosysJournalføringsoppgaverBehov() = OpprettGosysJournalføringsoppgaverMelding.behov(
            behovInput = OpprettGosysJournalføringsoppgaverMelding.BehovInput(
                identitetsnummer = fra,
                berørteIdentitetsnummer = setOf(til),
                journalpostIder = journalpostIder,
                journalpostType = "OverføreKoronaOmsorgsdager"
            )
        )
    }

    internal data class Løsningen(
        val bar: Boolean = false // TODO
    )

    internal data class Barn(
        internal val identitetsnummer: String,
        internal val fødselsdato: LocalDate,
        override val aleneOmOmsorgen: Boolean,
        override val utvidetRett: Boolean) : OmsorgsdagerBarn

    override fun validateBehov(packet: JsonMessage) {
        packet.requireValue(BehovKeys.Versjon, "1.0.0")
        packet.requireKey(
            BehovKeys.Barn,
            BehovKeys.OverførerFra,
            BehovKeys.OverførerTil,
            BehovKeys.OmsorgsdagerTattUtIÅr,
            BehovKeys.OmsorgsdagerÅOverføre,
            BehovKeys.JournalpostIder,
            BehovKeys.JobberINorge,
            BehovKeys.FraOgMed,
            BehovKeys.TilOgMed,
            BehovKeys.Mottatt
        )
    }

    override fun hentBehov(packet: JsonMessage) : Behovet {
        val mottatt = packet[BehovKeys.Mottatt].asText().let { ZonedDateTime.parse(it) }
        return Behovet(
            fra = packet[BehovKeys.OverførerFra].asText(),
            til = packet[BehovKeys.OverførerTil].asText(),
            jobberINorge = packet[BehovKeys.JobberINorge].asBoolean(),
            omsorgsdagerTattUtIÅr = packet[BehovKeys.OmsorgsdagerTattUtIÅr].asInt(),
            omsorgsdagerÅOverføre = packet[BehovKeys.OmsorgsdagerÅOverføre].asInt(),
            mottatt = mottatt,
            mottaksdato = mottatt.toLocalDateOslo(),
            journalpostIder = (packet[BehovKeys.JournalpostIder] as ArrayNode).map { it.asText() }.toSet(),
            periode = Periode(
                fom = packet[BehovKeys.FraOgMed].asLocalDate(),
                tom = packet[BehovKeys.TilOgMed].asLocalDate()
            ),
            barn = (packet[BehovKeys.Barn] as ArrayNode).map { Barn(
                identitetsnummer = it.get("identitetsnummer").asText(),
                fødselsdato = LocalDate.parse(it.get("fødselsdato").asText()),
                aleneOmOmsorgen = it.get("aleneOmOmsorgen").asBoolean(),
                utvidetRett = it.get("utvidetRett").asBoolean()
            )}
        )
    }

    override fun løsning(løsning: Løsningen): Pair<String, Map<String, *>> {
        return OverføreKoronaOmsorgsdager to mapOf( // TODO
            "versjon" to "1.0.0",
            "utfall" to "Gjennomført",
            "begrunnelser" to listOf<String>(),
            "overføringer" to mapOf<String, Any>()
        )
    }

    private object BehovKeys {
        val Versjon = "@behov.$OverføreKoronaOmsorgsdager.versjon"
        val Barn = "@behov.$OverføreKoronaOmsorgsdager.barn"
        val OverførerFra = "@behov.$OverføreKoronaOmsorgsdager.fra.identitetsnummer"
        val JobberINorge = "@behov.$OverføreKoronaOmsorgsdager.fra.jobberINorge"
        val OverførerTil = "@behov.$OverføreKoronaOmsorgsdager.til.identitetsnummer"
        val OmsorgsdagerTattUtIÅr = "@behov.$OverføreKoronaOmsorgsdager.omsorgsdagerTattUtIÅr"
        val OmsorgsdagerÅOverføre = "@behov.$OverføreKoronaOmsorgsdager.omsorgsdagerÅOverføre"
        val Mottatt = "@behov.$OverføreKoronaOmsorgsdager.mottatt"
        val JournalpostIder = "@behov.$OverføreKoronaOmsorgsdager.journalpostIder"
        val FraOgMed = "@behov.$OverføreKoronaOmsorgsdager.periode.fraOgMed"
        val TilOgMed = "@behov.$OverføreKoronaOmsorgsdager.periode.tilOgMed"
    }
}