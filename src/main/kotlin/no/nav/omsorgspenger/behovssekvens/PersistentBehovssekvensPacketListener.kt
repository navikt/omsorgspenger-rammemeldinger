package no.nav.omsorgspenger.behovssekvens

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import org.slf4j.Logger

internal abstract class PersistentBehovssekvensPacketListener(
    logger: Logger,
    private val steg: String,
    private val behovssekvensRepository: BehovssekvensRepository
) : BehovssekvensPacketListener(logger = logger) {

    override fun doHandlePacket(id: String, packet: JsonMessage) : Boolean {
        return behovssekvensRepository.skalHåndtere(
            behovssekvensId = id,
            steg = steg
        ).also { if (!it) {
            logger.warn("BehovssekvensId=$id har allerede vært gjennom steg=$steg.")
        }}
    }

    override fun onSent(id: String, packet: JsonMessage) {
        behovssekvensRepository.harHåndtert(
            behovssekvensId = id,
            behovssekvens = packet.toJson(),
            steg = steg
        )
    }
}