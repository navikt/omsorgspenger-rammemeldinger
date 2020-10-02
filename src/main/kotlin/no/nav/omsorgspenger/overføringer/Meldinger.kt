package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.k9.rapid.river.requireArray
import no.nav.k9.rapid.river.requireInt
import no.nav.k9.rapid.river.requireText
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding.Companion.erFordelingGirMelding
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding.Companion.somFordelingGirMelding
import no.nav.omsorgspenger.overføringer.Barn.Companion.erBarn
import no.nav.omsorgspenger.overføringer.Barn.Companion.somBarn
import no.nav.omsorgspenger.overføringer.Overføring.Companion.erOverføring
import no.nav.omsorgspenger.overføringer.Overføring.Companion.somOverføring
import no.nav.omsorgspenger.overføringer.Part.Companion.somPart
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak.Companion.erUtvidetRettVedtak
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak.Companion.somUtvidetRettVedtak
import java.time.LocalDate

interface Melding<Innhold> {
    fun validate()
    fun innhold() : Innhold
}

internal class OverføreOmsorgsdagerMelding(private val packet: JsonMessage) : Melding<OverføreOmsorgsdagerMelding.Innhold> {
    override fun validate() {
        packet.require(Barn) { json -> json.requireArray { entry -> entry.erBarn() } }
        packet.require(OverførerFra, JsonNode::requireText)
        packet.require(OverførerTil, JsonNode::requireText)
        packet.require(OmsorgsdagerTattUtIÅr, JsonNode::requireInt)
        packet.require(OmsorgsdagerÅOverføre, JsonNode::requireInt)
        packet.require(Mottaksdato, JsonNode::asLocalDate)
        packet.require(JournalpostIder) { json -> json.requireArray { entry -> entry is TextNode }}
    }

    override fun innhold() = Innhold(
        barn = (packet[Barn] as ArrayNode).map { it.somBarn() },
        overførerFra = packet[OverførerFra].textValue(),
        overførerTil = packet[OverførerTil].textValue(),
        omsorgsdagerTattUtIÅr = packet[OmsorgsdagerTattUtIÅr].asInt(),
        omsorgsdagerÅOverføre = packet[OmsorgsdagerÅOverføre].asInt(),
        mottaksdato = packet[Mottaksdato].asLocalDate(),
        journalpostIder = (packet[JournalpostIder] as ArrayNode).map { it.asText() }
    )

    internal companion object {
        internal const val Navn = "OverføreOmsorgsdager"
        private val Barn = "@behov.$Navn.barn"
        private val OverførerFra = "@behov.$Navn.fra.identitetsnummer"
        private val OverførerTil = "@behov.$Navn.til.identitetsnummer"
        private val OmsorgsdagerTattUtIÅr = "@behov.$Navn.omsorgsdagerTattUtIÅr"
        private val OmsorgsdagerÅOverføre = "@behov.$Navn.omsorgsdagerÅOverføre"
        private val Mottaksdato = "@behov.$Navn.mottaksdato"
        private val JournalpostIder = "@behov.$Navn.journalpostIder"
    }

    data class Innhold(
        val barn : List<Barn>,
        val overførerFra: String,
        val borINorge :Boolean = true, // TODO
        val jobberINorge: Boolean = true, // TODO
        val sendtPerBrev: Boolean = false, // TODO
        val overførerTil: String,
        val omsorgsdagerTattUtIÅr: Int,
        val omsorgsdagerÅOverføre: Int,
        val mottaksdato: LocalDate,
        val journalpostIder: List<String>) {

        internal val overordnetPeriode: Periode = {
            val sisteDatoMedOmsorgenFor = barn.sisteDatoMedOmsorgenFor()
            val tom = when {
                sisteDatoMedOmsorgenFor?.isAfter(mottaksdato) ?: false -> sisteDatoMedOmsorgenFor!!
                else -> mottaksdato
            }
            Periode(fom = mottaksdato, tom = tom)
        }()

        internal val ønskedeOverføringer = listOf(Overføring(
            periode = overordnetPeriode,
            antallDager = omsorgsdagerÅOverføre
        ))
    }
}

/**
 * TODO: Skal vi her kun melde behovet `FerdigstillOmsorgspengerJournalpost` som i sin tur har dette behovet?
 */
internal class HentOmsorgspengerSaksnummerMelding(private val packet: JsonMessage) : Melding<HentOmsorgspengerSaksnummerMelding.Innhold> {
    override fun validate() {
        packet.require(Saksnummer, JsonNode::requireText)
    }

    override fun innhold() = Innhold(
        saksnummer = packet[Saksnummer].asText()
    )

    internal companion object {
        internal const val Navn = "HentOmsorgspengerSaksnummer"
        private val Saksnummer = "@løsninger.$Navn.saksnummer"
    }

    data class Innhold(
        val saksnummer: String
    )
}

internal class FerdigstillJournalføringForOmsorgspengerMelding(private val packet: JsonMessage) : Melding<FerdigstillJournalføringForOmsorgspengerMelding.Innhold> {
    override fun validate() {
        packet.require(Saksnummer, JsonNode::requireText)
    }

    override fun innhold() = Innhold(
        saksnummer = packet[Saksnummer].asText()
    )

    fun input(journalpostIder: List<String>) = mapOf(
        "journalpostIder" to journalpostIder
    )

    internal companion object {
        internal const val Navn = "FerdigstillJournalføringForOmsorgspenger"
        private val Saksnummer = "@løsninger.$Navn.saksnummer"
        internal fun input(identitetsnummer: Identitetsnummer, journalpostIder: List<String>) = mapOf(
            "identitetsnummer" to identitetsnummer,
            "journalpostIder" to journalpostIder
        )
    }

    data class Innhold(
        val saksnummer: String
    )
}

internal class HentUtvidetRettVedtakMelding(private val packet: JsonMessage) : Melding<HentUtvidetRettVedtakMelding.Innhold> {

    override fun validate() {
        packet.interestedIn(Vedtak) { json -> json.requireArray { entry -> entry.erUtvidetRettVedtak() } }
    }

    override fun innhold() = Innhold(
        vedtak = (packet[Vedtak] as ArrayNode).map { it.somUtvidetRettVedtak() }
    )

    internal companion object {
        internal const val Navn = "HentUtvidetRettVedtak"
        internal fun input(identitetsnummer: Identitetsnummer, periode: Periode) = mapOf(
            "identitetsnummer" to identitetsnummer,
            "periode" to periode.toString()
        )

        private const val Vedtak = "@løsninger.$Navn.vedtak"
    }

    data class Innhold(
        val vedtak: List<UtvidetRettVedtak>
    )
}

internal class HentFordelingGirMeldingerMelding(private val packet: JsonMessage) : Melding<HentFordelingGirMeldingerMelding.Innhold> {

    override fun validate() {
        packet.interestedIn(Meldinger) { json -> json.requireArray { entry -> entry.erFordelingGirMelding() } }
    }

    override fun innhold() = Innhold(
        meldinger = (packet[Meldinger] as ArrayNode).map { it.somFordelingGirMelding() }
    )

    internal companion object {
        internal const val Navn = "HentFordelingGirMeldinger"
        internal fun input(identitetsnummer: Identitetsnummer, periode: Periode) = mapOf(
            "identitetsnummer" to identitetsnummer,
            "periode" to periode.toString()
        )

        private const val Meldinger = "@løsninger.$Navn.meldinger"
    }

    data class Innhold(
        val meldinger : List<FordelingGirMelding>
    )
}

internal class HentPersonopplysningerMelding(private val packet: JsonMessage) : Melding<HentPersonopplysningerMelding.Innhold> {
    override fun validate() {
        //packet.require(Identitetsnummer) { json -> json.requireObject { entry -> entry.fields().asSequence().all { it.toPair().erPart() }} }
        packet.interestedIn(Identitetsnummer)
    }

    override fun innhold() = Innhold(
        parter = (packet[Identitetsnummer] as ObjectNode).fields().asSequence().map { it.toPair().somPart() }.toSet()
    )

    internal companion object {
        internal const val Navn = "HentPersonopplysninger"
        internal fun input(identitetsnummer: Set<Identitetsnummer>) = mapOf(
            "identitetsnummer" to identitetsnummer,
            "attributter" to setOf("navn", "fødseldato")
        )

        private const val Identitetsnummer = "@løsninger.$Navn.identitetsnummer"
    }

    data class Innhold(
        internal val parter: Set<Part>
    )
}

internal class OverføreOmsorgsdagerBehandlingMelding(private val packet: JsonMessage) : Melding<OverføreOmsorgsdagerBehandlingMelding.Innhold> {
    override fun validate() {
        packet.require(Overføringer) { json -> json.requireArray { entry -> entry.erOverføring() } }
        packet.interestedIn(Karakteristikker)
    }

    override fun innhold() = Innhold(
        overføringer = (packet[Overføringer] as ArrayNode).map { it.somOverføring() },
        karakteristikker = (packet[Karakteristikker] as ArrayNode).map { Behandling.Karakteristikk.valueOf(it.asText()) }.toSet()
    )

    internal companion object {
        internal const val Navn = "OverføreOmsorgsdagerBehandling"
        private const val Overføringer = "@løsninger.$Navn.overføringer"
        private const val Karakteristikker = "@løsninger.$Navn.karakteristikker"
    }

    data class Innhold(
        val overføringer: List<Overføring>,
        val karakteristikker: Set<Behandling.Karakteristikk>
    )
}

internal class OpprettGosysJournalføringsoppgaverMelding(private val packet: JsonMessage) : Melding<Any?> {
    override fun validate() {}
    override fun innhold() = null

    internal companion object {
        internal const val Navn = "OpprettGosysJournalføringsoppgaver"
        private const val Overføringer = "@behov.$Navn.journalpostIder"

        fun input(journalpostIder: List<String>) = mapOf(
            "journalpostIder" to journalpostIder
        )
    }
}
