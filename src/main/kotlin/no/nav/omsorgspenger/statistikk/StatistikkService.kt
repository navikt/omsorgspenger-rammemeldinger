package no.nav.omsorgspenger.statistikk

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal class StatistikkService(
        private val kafkaProducer: KafkaProducer<String, String>,
        private val topic: String = "aapen-omsorgspengerRammemeldinger-statistikk-v1") {

    fun publiser(statistikk: OverføringStatistikkMelding) {
        kafkaProducer.send(ProducerRecord(topic, statistikk.toJson()))
    }
}