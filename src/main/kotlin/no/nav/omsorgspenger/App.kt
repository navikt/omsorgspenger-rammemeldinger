package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

fun main() {
    val env = System.getenv()
    val logger = LoggerFactory.getLogger("no.nav.omsorgspenger")

    RapidApplication.create(env).apply {
        RapidApp(this)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                logger.info("Startup achieved!")
            }
        })
    }.start()
}

internal class RapidApp(
        rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "my_event") }
            validate { it.requireKey("a_required_key") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        println(packet["a_required_key"].asText())
    }
}