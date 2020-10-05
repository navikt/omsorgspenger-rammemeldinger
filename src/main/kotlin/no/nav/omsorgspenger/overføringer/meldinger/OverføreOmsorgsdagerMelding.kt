package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Barn
import no.nav.omsorgspenger.overføringer.Barn.Companion.somBarn
import no.nav.omsorgspenger.overføringer.Overføring
import no.nav.omsorgspenger.overføringer.Utfall
import no.nav.omsorgspenger.overføringer.sisteDatoMedOmsorgenFor
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
            BehovKeys.Kilde
        )
        packet.requireAny(BehovKeys.Relasjon, gyldigeRelasjoner)
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
        val gitt = (løsning.parter.firstOrNull { it.identitetsnummer == løsning.til }?.let { overføringerTil(løsning.overføringer, it) })?: listOf()
        val fått = (løsning.parter.firstOrNull { it.identitetsnummer == løsning.fra }?.let { overføringerFra(løsning.overføringer, it) })?: listOf()
        val mappedOverføringer = mapOf<String, Any?>(
            løsning.fra to mapOf(
                "gitt" to gitt,
                "fått" to emptyList()
            ),
            løsning.til to mapOf(
                "fått" to fått,
                "gitt" to emptyList()
            )
        )

        return OverføreOmsorgsdager to mapOf(
            "utfall" to løsning.utfall.name,
            "begrunnelser" to løsning.begrunnelser,
            "overføringer" to when (løsning.utfall) {
                Utfall.Gjennomført, Utfall.Avslått -> mappedOverføringer
                else -> emptyMap()
            }
        )
    }

    private fun overføringerTil(overføringer: List<Overføring>, til: Part) = overføringer.map { mapOf(
        "antallDager" to it.antallDager,
        "gjelderFraOgMed" to it.periode.fom,
        "gjelderTilOgMed" to it.periode.tom,
        "til" to mapOf(
            "navn" to til.navn,
            "fødselsdato" to til.fødselsdato
        )
    )}

    private fun overføringerFra(overføringer: List<Overføring>, fra: Part) = overføringer.map { mapOf(
        "antallDager" to it.antallDager,
        "gjelderFraOgMed" to it.periode.fom,
        "gjelderTilOgMed" to it.periode.tom,
        "fra" to mapOf(
            "navn" to fra.navn,
            "fødselsdato" to fra.fødselsdato
        )
    )}

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

        internal val overordnetPeriode: Periode = {
            val sisteDatoMedOmsorgenFor = barn.sisteDatoMedOmsorgenFor()
            val tom = when {
                sisteDatoMedOmsorgenFor?.isAfter(mottaksdato) ?: false -> sisteDatoMedOmsorgenFor!!
                else -> mottaksdato
            }
            Periode(fom = mottaksdato, tom = tom)
        }()
    }

    internal data class Løsningen(
        internal val utfall: Utfall,
        internal val begrunnelser: List<String>,
        internal val fra: String,
        internal val til: String,
        internal val overføringer: List<Overføring>,
        internal val parter: Set<Part>
    )

    internal enum class Relasjon {
        NåværendeEktefelle,
        NåværendeSamboer
    }
    private val gyldigeRelasjoner = Relasjon.values().map { it.name }

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