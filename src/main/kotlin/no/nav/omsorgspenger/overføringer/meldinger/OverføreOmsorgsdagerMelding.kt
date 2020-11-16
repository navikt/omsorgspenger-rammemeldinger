package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.extensions.Oslo
import no.nav.omsorgspenger.extensions.toLocalDateOslo
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Barn
import no.nav.omsorgspenger.overføringer.Barn.Companion.sisteDatoMedOmsorgenFor
import no.nav.omsorgspenger.overføringer.Barn.Companion.somBarn
import no.nav.omsorgspenger.overføringer.Utfall
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

internal object OverføreOmsorgsdagerMelding :
    HentBehov<OverføreOmsorgsdagerMelding.Behovet>,
    LeggTilLøsning<OverføreOmsorgsdagerMelding.Løsningen> {
    internal const val OverføreOmsorgsdager = "OverføreOmsorgsdager"

    override fun validateBehov(packet: JsonMessage) {
        packet.requireKey(
            BehovKeys.Barn,
            BehovKeys.OverførerFra,
            BehovKeys.OverførerTil,
            BehovKeys.OmsorgsdagerTattUtIÅr,
            BehovKeys.OmsorgsdagerÅOverføre,
            BehovKeys.JournalpostIder,
            BehovKeys.JobberINorge,
            BehovKeys.Kilde,
            BehovKeys.Relasjon
        )
        packet.interestedIn(
            BehovKeys.HarBoddSammenMinstEttÅr, // Kun satt om relasjon er NåværendeSamboer
            BehovKeys.Mottaksdato, // TODO: Fjerne når i bruk i brukerdialog & punsj
            BehovKeys.Mottatt
        )
    }

    override fun hentBehov(packet: JsonMessage) = Relasjon.valueOf(packet[BehovKeys.Relasjon].asText()).let { relasjon ->


        Behovet(
            barn = (packet[BehovKeys.Barn] as ArrayNode).map { it.somBarn() },
            overførerFra = packet[BehovKeys.OverførerFra].textValue(),
            overførerTil = packet[BehovKeys.OverførerTil].textValue(),
            omsorgsdagerTattUtIÅr = packet[BehovKeys.OmsorgsdagerTattUtIÅr].asInt(),
            omsorgsdagerÅOverføre = packet[BehovKeys.OmsorgsdagerÅOverføre].asInt(),
            mottatt = packet.mottatt(),
            journalpostIder = (packet[BehovKeys.JournalpostIder] as ArrayNode).map { it.asText() }.toSet(),
            jobberINorge = packet[BehovKeys.JobberINorge].asBoolean(),
            sendtPerBrev = packet[BehovKeys.Kilde].asText().equals(other = "Brev", ignoreCase = true),
            relasjon = relasjon,
            harBoddSammentMinstEttÅr = when (relasjon) {
                Relasjon.NåværendeSamboer -> packet[BehovKeys.HarBoddSammenMinstEttÅr].asBoolean()
                else -> null
            }
        )
    }

    private fun JsonMessage.mottatt() = kotlin.runCatching { get(BehovKeys.Mottatt).asText().let {
        ZonedDateTime.parse(it)
    }}.fold(
        onSuccess = { it },
        onFailure = {
            val mottaksdato = get(BehovKeys.Mottaksdato).asLocalDate()
            ZonedDateTime.of(mottaksdato, LocalTime.now(Oslo), Oslo)
        }
    )

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

        return OverføreOmsorgsdager to mapOf(
            "versjon" to "1.0.0",
            "utfall" to løsning.utfall.name,
            "begrunnelser" to listOf<String>(),
            "overføringer" to overføringer
        )
    }

    private fun Personopplysninger.navnTilLøsning() = when (navn) {
        null -> null
        else -> mapOf(
            "fornavn" to navn.fornavn,
            "mellomnavn" to navn.mellomnavn,
            "etternavn" to navn.etternavn
        )
    }

    internal data class Behovet(
        val barn : List<Barn>,
        val overførerFra: String,
        val jobberINorge: Boolean,
        val sendtPerBrev: Boolean,
        val overførerTil: String,
        val omsorgsdagerTattUtIÅr: Int,
        val omsorgsdagerÅOverføre: Int,
        val mottatt: ZonedDateTime,
        val mottaksdato: LocalDate = mottatt.toLocalDateOslo(),
        val journalpostIder: Set<String>,
        val relasjon: Relasjon,
        val harBoddSammentMinstEttÅr: Boolean?) {
        val overordnetPeriode : Periode
        val overordnetPeriodeUtledetFraBarnMedUtvidetRett : Boolean

        init {
            val (sisteDatoMedOmsorgenFor, utledetFraBarnMedUtvidetRett) =
                barn.sisteDatoMedOmsorgenFor()?:(mottaksdato to false)

            when {
                sisteDatoMedOmsorgenFor.isBefore(mottaksdato) -> {
                    overordnetPeriode = Periode(fom = mottaksdato, tom = mottaksdato)
                    overordnetPeriodeUtledetFraBarnMedUtvidetRett = false
                }
                else -> {
                    overordnetPeriode = Periode(fom = mottaksdato, tom = sisteDatoMedOmsorgenFor)
                    overordnetPeriodeUtledetFraBarnMedUtvidetRett = utledetFraBarnMedUtvidetRett
                }
            }
        }
    }

    internal data class Løsningen(
        internal val utfall: Utfall,
        internal val gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer>,
        private val personopplysninger: Map<Identitetsnummer, Personopplysninger>,
        private val alleSaksnummerMapping: Map<Identitetsnummer, Saksnummer>) {
        private val saksnummerTilIdentitetsnummer = alleSaksnummerMapping.entries.associate{(k,v)-> v to k}
        internal fun personopplysninger(sak: Saksnummer) =
            personopplysninger.getValue(saksnummerTilIdentitetsnummer.getValue(sak))
        internal fun identitetsnummer(sak: Saksnummer) =
            personopplysninger(sak).gjeldendeIdentitetsnummer
    }

    internal enum class Relasjon {
        NåværendeEktefelle,
        NåværendeSamboer
    }

    private object BehovKeys {
        val Barn = "@behov.$OverføreOmsorgsdager.barn"
        val OverførerFra = "@behov.$OverføreOmsorgsdager.fra.identitetsnummer"
        val JobberINorge = "@behov.$OverføreOmsorgsdager.fra.jobberINorge"
        val OverførerTil = "@behov.$OverføreOmsorgsdager.til.identitetsnummer"
        val OmsorgsdagerTattUtIÅr = "@behov.$OverføreOmsorgsdager.omsorgsdagerTattUtIÅr"
        val OmsorgsdagerÅOverføre = "@behov.$OverføreOmsorgsdager.omsorgsdagerÅOverføre"
        val Mottaksdato = "@behov.$OverføreOmsorgsdager.mottaksdato"
        val Mottatt = "@behov.$OverføreOmsorgsdager.mottatt"
        val JournalpostIder = "@behov.$OverføreOmsorgsdager.journalpostIder"
        val Kilde = "@behov.$OverføreOmsorgsdager.kilde"
        val Relasjon = "@behov.$OverføreOmsorgsdager.til.relasjon"
        val HarBoddSammenMinstEttÅr = "@behov.$OverføreOmsorgsdager.til.harBoddSammenMinstEttÅr"
    }
}