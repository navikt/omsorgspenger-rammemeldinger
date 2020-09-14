package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behovsformat
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.sendMedId
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.overføringer.Barn.Companion.somBarn
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OmsorgspengerRammemeldinger(rapidsConnection: RapidsConnection) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(Behov)
                it.requireAll(Behovsformat.Behovsrekkefølge, listOf(Behov))
            }
            validate {
                it.require(OmsorgsdagerTattUtIÅr, JsonNode::asInt)
                it.require(OmsorgsdagerÅOverføre, JsonNode::asInt)
                it.require(OverførerFra, JsonNode::asText)
                it.require(OverførerTil, JsonNode::asText)
                it.require(Barn, JsonNode::isArray)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        logger.info("Skal løse behov $Behov med id $id")

        val omsorgsdagerTattUtIÅr = packet[OmsorgsdagerTattUtIÅr].asInt()
        val omsorgsdagerÅOverføre = packet[OmsorgsdagerÅOverføre].asInt()
        val fra = packet[OverførerFra].asText()
        val til = packet[OverførerTil].asText()
        val barn = (packet[Barn] as ArrayNode).map { it.somBarn() }

        val omsorgsdagerIgjen = 10 - omsorgsdagerTattUtIÅr

        val (utfall, begrunnelser) = when {
            omsorgsdagerÅOverføre !in 1..10 -> Pair("Avslått", listOf("Kan ikke overføre $omsorgsdagerÅOverføre dager. Må være melom 1 og 10 dager."))
            omsorgsdagerIgjen < omsorgsdagerÅOverføre -> Pair("Avslått", listOf("Har kun $omsorgsdagerIgjen dager igjen i år. Kan ikke overføre $omsorgsdagerÅOverføre dager."))
            barn.any { it.utvidetRett } -> Pair("OppgaveIGosysOgBehandlesIInfotrygd", listOf("Overføringen kan ikke behandles i nytt system."))
            else -> Pair("Gjennomført", listOf("Overføringen er effektuert."))
        }

        logger.info("Utfall=$utfall, Begrunnelser=$begrunnelser")

        packet.leggTilLøsning(Behov, mockLøsning(
            utfall = utfall,
            begrunnelser = begrunnelser,
            fra = fra,
            til = til,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
        ))
        context.sendMedId(packet)
    }

    internal companion object {
        const val Behov = "OverføreOmsorgsdager"
        internal val OmsorgsdagerTattUtIÅr = "@behov.$Behov.omsorgsdagerTattUtIÅr"
        internal val OmsorgsdagerÅOverføre = "@behov.$Behov.omsorgsdagerÅOverføre"
        internal val OverførerFra = "@behov.$Behov.fra.identitetsnummer"
        internal val OverførerTil = "@behov.$Behov.til.identitetsnummer"
        internal val Barn = "@behov.$Behov.barn"

        private fun mockLøsning(
            utfall: String,
            begrunnelser: List<String>,
            fra: String,
            til: String,
            omsorgsdagerÅOverføre: Int
        ) : Map<String, Any?> {
            val overføringer = mapOf(
                fra to mapOf(
                    "gitt" to listOf(
                        mapOf(
                            "antallDager" to omsorgsdagerÅOverføre,
                            "gjelderFraOgMed" to LocalDate.now(),
                            "gjelderTilOgMed" to LocalDate.now().plusYears(1),
                            "til" to mapOf(
                                "navn" to "Kari Nordmann",
                                "fødselsdato" to LocalDate.now().minusYears(30)
                            )
                        )
                    ),
                    "fått" to emptyList()
                ),
                til to mapOf(
                    "fått" to listOf(
                        mapOf(
                            "antallDager" to omsorgsdagerÅOverføre,
                            "gjelderFraOgMed" to LocalDate.now(),
                            "gjelderTilOgMed" to LocalDate.now().plusYears(1),
                            "fra" to mapOf(
                                "navn" to "Ola Nordmann",
                                "fødselsdato" to LocalDate.now().minusYears(35)
                            )
                        )
                    ),
                    "gitt" to emptyList()
                )
            )

            return mapOf(
                "utfall" to utfall,
                "begrunnelser" to begrunnelser,
                "overføringer" to when (utfall) {
                    "Gjennomført", "Avslått" -> overføringer
                    else -> emptyMap()
                }
            )
        }

    }
}