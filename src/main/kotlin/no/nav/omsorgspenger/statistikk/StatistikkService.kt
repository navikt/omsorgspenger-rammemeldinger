package no.nav.omsorgspenger.statistikk

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal interface StatistikkService {
    fun publiser(statistikkMelding: StatistikkMelding)
}

internal class KafkaStatistikkService(
        private val kafkaProducer: KafkaProducer<String, String>,
        private val topic: String = "aapen-omsorgspengerRammemeldinger-statistikk-v1",
        private val enabled: Boolean) : StatistikkService {

    override fun publiser(statistikkMelding: StatistikkMelding) {
        if(!enabled) {
            return
        }
        kafkaProducer.send(ProducerRecord(topic, statistikkMelding.toJson()))
    }
}