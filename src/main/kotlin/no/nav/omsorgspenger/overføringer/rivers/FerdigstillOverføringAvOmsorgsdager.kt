package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class FerdigstillOverføringAvOmsorgsdager(rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.interestedIn("@id")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        logger.info("$id -> FerdigstillOverføringAvOmsorgsdager")

        logger.info("Alle behov meldt i 'BehanleOmsorgspengerOverføring' etter at løsning er lagt til på overføring bør ferdigstilles her.")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(FerdigstillOverføringAvOmsorgsdager::class.java)
    }

}