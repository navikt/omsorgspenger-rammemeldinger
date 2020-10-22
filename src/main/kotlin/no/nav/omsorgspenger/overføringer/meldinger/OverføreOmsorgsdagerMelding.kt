package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Barn
import no.nav.omsorgspenger.overføringer.Barn.Companion.sisteDatoMedOmsorgenFor
import no.nav.omsorgspenger.overføringer.Barn.Companion.somBarn
import no.nav.omsorgspenger.overføringer.Utfall
import java.time.LocalDate

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
            BehovKeys.Mottaksdato,
            BehovKeys.JournalpostIder,
            BehovKeys.JobberINorge,
            BehovKeys.BorINorge,
            BehovKeys.Kilde,
            BehovKeys.Relasjon
        )
        packet.interestedIn(
            BehovKeys.HarBoddSammenMinstEttÅr // Kun satt om relasjon er NåværendeSamboer
        )
    }

    override fun hentBehov(packet: JsonMessage) = Relasjon.valueOf(packet[BehovKeys.Relasjon].asText()).let { relasjon ->
        Behovet(
            barn = (packet[BehovKeys.Barn] as ArrayNode).map { it.somBarn() },
            overførerFra = packet[BehovKeys.OverførerFra].textValue(),
            overførerTil = packet[BehovKeys.OverførerTil].textValue(),
            omsorgsdagerTattUtIÅr = packet[BehovKeys.OmsorgsdagerTattUtIÅr].asInt(),
            omsorgsdagerÅOverføre = packet[BehovKeys.OmsorgsdagerÅOverføre].asInt(),
            mottaksdato = packet[BehovKeys.Mottaksdato].asLocalDate(),
            journalpostIder = (packet[BehovKeys.JournalpostIder] as ArrayNode).map { it.asText() }.toSet(),
            jobberINorge = packet[BehovKeys.JobberINorge].asBoolean(),
            borINorge = packet[BehovKeys.BorINorge].asBoolean(),
            sendtPerBrev = packet[BehovKeys.Kilde].asText().equals(other = "Brev", ignoreCase = true),
            relasjon = relasjon,
            harBoddSammentMinstEttÅr = when (relasjon) {
                Relasjon.NåværendeSamboer -> packet[BehovKeys.HarBoddSammenMinstEttÅr].asBoolean()
                else -> null
            }
        )
    }

    override fun løsning(løsning: Løsningen): Pair<String, Map<String, *>> {
        val overføringer = løsning.gjeldendeOverføringer
            .mapValues { (_,gjeldendeOverføringer) ->
                mapOf(
                    "gitt" to gjeldendeOverføringer.gitt.map { gitt -> mapOf(
                        "antallDager" to gitt.antallDager,
                        "gjelderFraOgMed" to gitt.periode.fom,
                        "gjelderTilOgMed" to gitt.periode.tom,
                        "til" to mapOf(
                            "navn" to løsning.personopplysninger.getValue(gitt.til.identitetsnummer).navn,
                            "fødselsdato" to løsning.personopplysninger.getValue(gitt.til.identitetsnummer).fødselsdato.toString()
                        )
                    )},
                    "fått" to gjeldendeOverføringer.fått.map { fått -> mapOf(
                        "antallDager" to fått.antallDager,
                        "gjelderFraOgMed" to fått.periode.fom,
                        "gjelderTilOgMed" to fått.periode.tom,
                        "fra" to mapOf(
                            "navn" to løsning.personopplysninger.getValue(fått.fra.identitetsnummer).navn,
                            "fødselsdato" to løsning.personopplysninger.getValue(fått.fra.identitetsnummer).fødselsdato.toString()
                        )
                    )}
                )
            }

        return OverføreOmsorgsdager to mapOf(
            "utfall" to løsning.utfall.name,
            "begrunnelser" to listOf<String>(),
            "overføringer" to overføringer
        )
    }

    internal data class Behovet(
        val barn : List<Barn>,
        val overførerFra: String,
        val borINorge: Boolean,
        val jobberINorge: Boolean,
        val sendtPerBrev: Boolean,
        val overførerTil: String,
        val omsorgsdagerTattUtIÅr: Int,
        val omsorgsdagerÅOverføre: Int,
        val mottaksdato: LocalDate,
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
        internal val gjeldendeOverføringer: Map<Identitetsnummer, GjeldendeOverføringer>,
        internal val personopplysninger: Map<Identitetsnummer, Personopplysninger>
    )

    internal enum class Relasjon {
        NåværendeEktefelle,
        NåværendeSamboer
    }

    private object BehovKeys {
        val Barn = "@behov.$OverføreOmsorgsdager.barn"
        val OverførerFra = "@behov.$OverføreOmsorgsdager.fra.identitetsnummer"
        val BorINorge = "@behov.$OverføreOmsorgsdager.fra.borINorge"
        val JobberINorge = "@behov.$OverføreOmsorgsdager.fra.jobberINorge"
        val OverførerTil = "@behov.$OverføreOmsorgsdager.til.identitetsnummer"
        val OmsorgsdagerTattUtIÅr = "@behov.$OverføreOmsorgsdager.omsorgsdagerTattUtIÅr"
        val OmsorgsdagerÅOverføre = "@behov.$OverføreOmsorgsdager.omsorgsdagerÅOverføre"
        val Mottaksdato = "@behov.$OverføreOmsorgsdager.mottaksdato"
        val JournalpostIder = "@behov.$OverføreOmsorgsdager.journalpostIder"
        val Kilde = "@behov.$OverføreOmsorgsdager.kilde"
        val Relasjon = "@behov.$OverføreOmsorgsdager.til.relasjon"
        val HarBoddSammenMinstEttÅr = "@behov.$OverføreOmsorgsdager.til.harBoddSammenMinstEttÅr"
    }
}