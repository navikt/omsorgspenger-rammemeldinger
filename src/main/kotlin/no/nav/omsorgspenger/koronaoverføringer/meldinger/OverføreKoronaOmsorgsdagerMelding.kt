package no.nav.omsorgspenger.koronaoverføringer.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.extensions.sisteDagIÅret
import no.nav.omsorgspenger.extensions.toLocalDateOslo
import no.nav.omsorgspenger.omsorgsdager.OmsorgsdagerBarn
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.Utfall
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
        internal val utfall: Utfall,
        internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
        private val personopplysninger: Map<Identitetsnummer, OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger>,
        private val alleSaksnummerMapping: Map<Identitetsnummer, Saksnummer>) {
        private val saksnummerTilIdentitetsnummer = alleSaksnummerMapping.entries.associate { (k, v) -> v to k }
        internal fun personopplysninger(sak: Saksnummer) =
            personopplysninger.getValue(saksnummerTilIdentitetsnummer.getValue(sak))
        internal fun identitetsnummer(sak: Saksnummer) =
            personopplysninger(sak).gjeldendeIdentitetsnummer
        internal companion object {
            internal val GosysJournalføringsoppgaver = Løsningen(
                utfall = Utfall.GosysJournalføringsoppgaver,
                gjeldendeOverføringer = emptyMap(),
                alleSaksnummerMapping = emptyMap(),
                personopplysninger = emptyMap()
            )
        }
    }

    internal data class Barn(
        internal val identitetsnummer: String,
        internal val fødselsdato: LocalDate,
        override val aleneOmOmsorgen: Boolean,
        override val utvidetRett: Boolean) : OmsorgsdagerBarn {
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

        val overføringer = løsning.gjeldendeOverføringer
            .mapKeys { (saksnummer) -> løsning.identitetsnummer(saksnummer) }
            .mapValues { (_,gjeldendeOverføringer) ->
                mapOf(
                    "gitt" to gjeldendeOverføringer.gitt.map { gitt -> mapOf(
                        "antallDager" to gitt.antallDager,
                        "gjelderFraOgMed" to gitt.periode.fom,
                        "gjelderTilOgMed" to gitt.periode.tom,
                        "til" to mapOf(
                            "navn" to løsning.personopplysninger(gitt.til).navnTilLøsning(),
                            "fødselsdato" to løsning.personopplysninger(gitt.til).fødselsdato.toString()
                        )
                    )},
                    "fått" to gjeldendeOverføringer.fått.map { fått -> mapOf(
                        "antallDager" to fått.antallDager,
                        "gjelderFraOgMed" to fått.periode.fom,
                        "gjelderTilOgMed" to fått.periode.tom,
                        "fra" to mapOf(
                            "navn" to løsning.personopplysninger(fått.fra).navnTilLøsning(),
                            "fødselsdato" to løsning.personopplysninger(fått.fra).fødselsdato.toString()
                        )
                    )}
                )
            }

        return OverføreKoronaOmsorgsdager to mapOf(
            "versjon" to "1.0.0",
            "utfall" to løsning.utfall.name,
            "begrunnelser" to listOf<String>(),
            "overføringer" to overføringer
        )
    }

    private fun OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger.navnTilLøsning() = when (navn) {
        null -> null
        else -> mapOf(
            "fornavn" to navn.fornavn,
            "mellomnavn" to navn.mellomnavn,
            "etternavn" to navn.etternavn
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