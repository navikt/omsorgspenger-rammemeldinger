package no.nav.omsorgspenger.statistikk

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

internal class StatistikkServiceTest {
    private val kafkaProducer = mockk<KafkaProducer<String, String>>()
    private val topic = "topic"

    private val statistikkService = StatistikkService(
            kafkaProducer = kafkaProducer,
            topic = topic,
            enabled = true
    )

    @BeforeEach
    internal fun setUp() {
        clearMocks(kafkaProducer)
    }

    @Test
    internal fun `send melding uten skjerming`() {
        val meldinger = mutableListOf<ProducerRecord<String, String>>()

        every { kafkaProducer.send(capture(meldinger)) }.returns(CompletableFuture.completedFuture(null))

        statistikkService.publiser(false, melding)

        assertThat(meldinger.map { it.topic() }).containsExactly(topic)
        assertThat(meldinger.map { StatistikkMelding.fromJson(it.value()) }).containsExactly(melding)
    }

    @Test
    internal fun `send melding med skjerming`() {
        val meldinger = mutableListOf<ProducerRecord<String, String>>()

        every { kafkaProducer.send(capture(meldinger)) }.returns(CompletableFuture.completedFuture(null))


        val skjermet = true
        statistikkService.publiser(skjermet, melding)

        assertThat(meldinger.map { it.topic() }).containsExactly(topic)

        assertThat(meldinger).hasSize(1)

        val sentMelding = StatistikkMelding.fromJson(meldinger[0].value())
        assertThat(sentMelding.aktorId).isEqualTo("-5")
        assertThat(sentMelding.ansvarligEnhetKode).isEqualTo("-5")
        assertThat(sentMelding.behandlendeEnhetKode).isEqualTo("-5")
    }

    val melding = StatistikkMelding(
            saksnummer = "",
            behandlingId = "",
            mottattDato = LocalDate.parse("2020-01-01"),
            registrertDato = LocalDate.parse("2020-01-01"),
            behandlingType = "",
            behandlingStatus = "",
            funksjonellTid = OffsetDateTime.now(),
            aktorId = "12345678",
            tekniskTid = OffsetDateTime.now(),
            ansvarligEnhetKode = ""
    )
}