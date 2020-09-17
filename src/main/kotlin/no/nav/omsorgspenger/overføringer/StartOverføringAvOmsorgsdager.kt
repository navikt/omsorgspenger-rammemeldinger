package no.nav.omsorgspenger.overføringer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.overføringer.Barn.Companion.erBarn
import no.nav.omsorgspenger.overføringer.Barn.Companion.somBarn
import no.nav.omsorgspenger.overføringer.Behov.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.overføringer.Behov.OverføreOmsorgsdager
import no.nav.omsorgspenger.overføringer.MockLøsning.mockLøsning
import org.slf4j.LoggerFactory

internal class StartOverføringAvOmsorgsdager(rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.utenLøsningPåBehov(HentOmsorgspengerSaksnummer)
            }
            validate {
                it.require(OmsorgsdagerTattUtIÅr, JsonNode::requireInt)
                it.require(OmsorgsdagerÅOverføre, JsonNode::requireInt)
                it.require(OverførerFra, JsonNode::requireText)
                it.require(OverførerTil, JsonNode::requireText)
                it.require(Barn) { json -> json.requireArray { entry -> entry.erBarn() } }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        logger.info("Skal løse behov $OverføreOmsorgsdager med id $id")

        val omsorgsdagerTattUtIÅr = packet[OmsorgsdagerTattUtIÅr].asInt()
        val omsorgsdagerÅOverføre = packet[OmsorgsdagerÅOverføre].asInt()
        val fra = packet[OverførerFra].asText()
        val til = packet[OverførerTil].asText()
        val barn = (packet[Barn] as ArrayNode).map { it.somBarn() }

        val omsorgsdagerIgjen = 10 - omsorgsdagerTattUtIÅr

        when {
            omsorgsdagerÅOverføre !in 1..10 -> Pair("Avslått", listOf("Kan ikke overføre $omsorgsdagerÅOverføre dager. Må være melom 1 og 10 dager."))
            omsorgsdagerIgjen < omsorgsdagerÅOverføre -> Pair("Avslått", listOf("Har kun $omsorgsdagerIgjen dager igjen i år. Kan ikke overføre $omsorgsdagerÅOverføre dager."))
            barn.any { it.utvidetRett } -> Pair("OppgaveIGosysOgBehandlesIInfotrygd", listOf("Overføringen kan ikke behandles i nytt system."))
            else -> null
        }?.also { (utfall, begrunnelser) ->
            logger.info("Utfall=$utfall, Begrunnelser=$begrunnelser")

            packet.leggTilLøsning(OverføreOmsorgsdager, mockLøsning(
                utfall = utfall,
                begrunnelser = begrunnelser,
                fra = fra,
                til = til,
                omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
            ))
            context.sendMedId(packet)
            return
        }

        logger.info("Legger til behov for å hente saksnummer for omsorgspenger.")

        packet.leggTilBehov(aktueltBehov = OverføreOmsorgsdager, Behov(
            navn = HentOmsorgspengerSaksnummer,
            input = mapOf(
                "identitetsnummer" to fra
            )
        ))

        context.sendMedId(packet)
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(StartOverføringAvOmsorgsdager::class.java)
        private val OmsorgsdagerTattUtIÅr = "@behov.${OverføreOmsorgsdager}.omsorgsdagerTattUtIÅr"
        internal val OmsorgsdagerÅOverføre = "@behov.${OverføreOmsorgsdager}.omsorgsdagerÅOverføre"
        internal val OverførerFra = "@behov.${OverføreOmsorgsdager}.fra.identitetsnummer"
        internal val OverførerTil = "@behov.${OverføreOmsorgsdager}.til.identitetsnummer"
        private val Barn = "@behov.${OverføreOmsorgsdager}.barn"
    }
}