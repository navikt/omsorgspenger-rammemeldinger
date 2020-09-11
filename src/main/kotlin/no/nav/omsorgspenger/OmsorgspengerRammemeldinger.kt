package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.skalLøseBehov
import org.slf4j.LoggerFactory

internal class OmsorgspengerRammemeldinger(rapidsConnection: RapidsConnection) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(behov)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        logger.info("Skall løse behov")

        val id = packet["@id"].asText()
        val løsning = mapOf(
                "utfall" to "gjennomført",
                "begrunnelser" to listOf("øverføringen ær gjennomført"),
                "overføringer" to mapOf(
                        "Gry" to "Øverføring",
                        "Ola" to "Øverføring")
        )

        packet.leggTilLøsning(behov, løsning)
        context.send(id, packet.toJson())
    }

    internal companion object {
        internal const val behov = "OverføreOmsorgsdager"
    }
}