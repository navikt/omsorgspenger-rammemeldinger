package no.nav.omsorgspenger.overføringer.rivers

/*
internal class FerdigstillOverføringAvOmsorgsdager(rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.harLøsningPåBehov(HentOmsorgspengerSaksnummer)
            }
            validate {
                it.require(Saksnummer, JsonNode::requireText)
                it.require(OmsorgsdagerÅOverføre, JsonNode::requireInt)
                it.require(OverførerFra, JsonNode::requireText)
                it.require(OverførerTil, JsonNode::requireText)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        logger.info("Skal løse behov $OverføreOmsorgsdager med id $id")

        val saksnummer = packet[Saksnummer].textValue()
        logger.info("Behandles på saksnummer $saksnummer")

        val omsorgsdagerÅOverføre = packet[OmsorgsdagerÅOverføre].asInt()
        val fra = packet[OverførerFra].asText()
        val til = packet[OverførerTil].asText()

        val utfall = "Gjennomført"
        val begrunnelser = listOf("Overføringen effektueres")
        logger.info("Utfall=$utfall, Begrunnelser=$begrunnelser")

        packet.leggTilLøsning(OverføreOmsorgsdager, mockLøsning(
            utfall = utfall,
            begrunnelser = begrunnelser,
            fra = fra,
            til = til,
            omsorgsdagerÅOverføre = omsorgsdagerÅOverføre
        ))
        context.sendMedId(packet)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(FerdigstillOverføringAvOmsorgsdager::class.java)
        private const val Saksnummer = "@løsninger.$HentOmsorgspengerSaksnummer.saksnummer"
    }

}*/