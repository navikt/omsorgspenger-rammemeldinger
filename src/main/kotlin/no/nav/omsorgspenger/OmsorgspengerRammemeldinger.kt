package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.skalLøseBehov
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZonedDateTime

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

        val json = jacksonObjectMapper.readTree(packet.toJson().toString())
        val id = json.get("@id").asText()

        val behovFra = json.get("@behov").get(behov).get("fra")
        val behovTil = json.get("@behov").get(behov).get("til")

        println("json: $json")

        val behov = json.get("@behov").get(behov).asText()

        val løsning = mapOf(
                "løst" to ZonedDateTime.now().toString(),
                "utfall" to "gjennomført",
                "begrunnelser" to listOf("gjennomført."),
                "øverføringer" to mapOf(
                        "Gry" to mapOf(
                                "fra" to mapOf(
                                        "navn" to "Ola",
                                        "fødselsdato" to behovFra.get("identitetsnummer")
                                ),
                                "antallDager" to "3",
                                "gjelderFraOgMed" to LocalDate.now().toString(),
                                "gjelderTilOgMed" to LocalDate.now().plusDays(50)
                        )
                )
        )

        packet.leggTilLøsning(behov, løsning)
        context.send(id, packet.toJson())
    }

    internal companion object {
        const val behov = "OverføreOmsorgsdager"
        val jacksonObjectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

}