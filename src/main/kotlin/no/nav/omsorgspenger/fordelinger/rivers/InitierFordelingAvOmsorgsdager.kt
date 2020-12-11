package no.nav.omsorgspenger.fordelinger.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.leggTilBehovEtter
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.fordelinger.rivers.FordelingAvOmsorgsdagerMelding.FordelingAvOmsorgsdager
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding
import org.slf4j.LoggerFactory

internal class InitierFordelingAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "InitierFordeleOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierFordelingAvOmsorgsdager::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(FordelingAvOmsorgsdager)
                FordelingAvOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage) : Boolean{

        val behovet = FordelingAvOmsorgsdagerMelding.hentBehov(packet)

        packet.leggTilLøsningPar(FordelingAvOmsorgsdagerMelding.løsning(FordelingAvOmsorgsdagerMelding.Løsningen()))

        logger.info("Legger til behovet ${OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver}")

        packet.leggTilBehovEtter(FordelingAvOmsorgsdager, OpprettGosysJournalføringsoppgaverMelding.behov(
            OpprettGosysJournalføringsoppgaverMelding.BehovInput(
                identitetsnummer = behovet.fra,
                berørteIdentitetsnummer = setOf(behovet.til).plus(behovet.barn),
                journalpostIder = behovet.journalpostIder,
                journalpostType = "FordeleOmsorgsdager"
            )
        ))

        return true
    }
}