package no.nav.omsorgspenger.midlertidigalene.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.leggTilBehovEtter
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import org.slf4j.LoggerFactory

internal class InitierMidlertidigAlene(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "InitierMidlertidigAlene",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierMidlertidigAlene::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(MidlertidigAleneMelding.MidlertidigAlene)
                MidlertidigAleneMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage) : Boolean{
        logger.warn("Håndtering av behov ${MidlertidigAleneMelding.MidlertidigAlene} bør flyttes til 'omsorgspenger-rammevedtak'")

        val behovet = MidlertidigAleneMelding.hentBehov(packet)

        packet.leggTilLøsningPar(MidlertidigAleneMelding.løsning(MidlertidigAleneMelding.Løsningen()))

        logger.info("Legger til behovet ${OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver}")

        packet.leggTilBehovEtter(MidlertidigAleneMelding.MidlertidigAlene, OpprettGosysJournalføringsoppgaverMelding.behov(
            OpprettGosysJournalføringsoppgaverMelding.BehovInput(
                søker = behovet.søker,
                annenForelder = behovet.annenForelder,
                journalpostIder = behovet.journalpostIder
            )
        ))

        return true
    }
}