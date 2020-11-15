package no.nav.omsorgspenger.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import org.slf4j.Logger

internal abstract class PersistentBehovssekvensPacketListener(
    logger: Logger,
    private val håndtertStatus: String,
    private val behovssekvensRepository: BehovssekvensRepository
) : BehovssekvensPacketListener(logger = logger) {

    override fun doHandlePacket(id: String, packet: JsonMessage) : Boolean {
        return behovssekvensRepository.skalHåndtere(
            behovssekvensId = id,
            status = håndtertStatus
        )
    }

    override fun onSent(id: String, packet: JsonMessage) {
        behovssekvensRepository.harHåndtert(
            behovssekvensId = id,
            behovssekvens = packet.toJson(),
            status = håndtertStatus
        )
    }
}