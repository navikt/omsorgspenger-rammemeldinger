package no.nav.omsorgspenger.statistikk

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal class StatistikkService(
        private val kafkaProducer: KafkaProducer<String, String>,
        private val topic: String = "aapen-omsorgspengerRammemeldinger-statistikk-v1",
        private val enabled: Boolean
) {

    fun publiser(skjermet: Boolean, statistikk: StatistikkMelding) {
        if(!enabled) {
            return
        }

        val melding = if(skjermet) statistikk.utenSkjermedeFelt() else statistikk

        kafkaProducer.send(ProducerRecord(topic, melding.toJson()))
    }
}