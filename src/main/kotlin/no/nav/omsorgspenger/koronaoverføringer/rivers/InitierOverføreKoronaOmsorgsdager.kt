package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.leggTilBehovEtter
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.k9.rapid.river.utenLøsningPåBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.koronaoverføringer.Perioder.erStøttetPeriode
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding
import org.slf4j.LoggerFactory

internal class InitierOverføreKoronaOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "InitierOverføreKoronaOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierOverføreKoronaOmsorgsdager::class.java)) {

    private val aktueltBehov = OverføreKoronaOmsorgsdagerMelding.OverføreKoronaOmsorgsdager

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(aktueltBehov)
                it.utenLøsningPåBehov(HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer)
                OverføreKoronaOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val overføringen = OverføreKoronaOmsorgsdagerMelding.hentBehov(packet)

        if (overføringen.periode.erStøttetPeriode()) {
            logger.info("Legger til behov ${HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer}")
            packet.leggTilBehov(
                aktueltBehov = aktueltBehov,
                behov = arrayOf(HentOmsorgspengerSaksnummerMelding.behov(
                    HentOmsorgspengerSaksnummerMelding.BehovInput(
                        identitetsnummer = setOf(overføringen.fra, overføringen.til)
                    )
                ))
            )
        } else {
            packet.leggTilLøsningPar(OverføreKoronaOmsorgsdagerMelding.løsning(
                OverføreKoronaOmsorgsdagerMelding.Løsningen()
            ))
            packet.leggTilBehovEtter(
                aktueltBehov = aktueltBehov,
                behov = arrayOf(OpprettGosysJournalføringsoppgaverMelding.behov(
                    behovInput = OpprettGosysJournalføringsoppgaverMelding.BehovInput(
                        identitetsnummer = overføringen.fra,
                        berørteIdentitetsnummer = setOf(overføringen.til),
                        journalpostIder = overføringen.journalpostIder,
                        journalpostType = "OverføreKoronaOmsorgsdager" // TODO: Må legges til i journalføring
                    )
                ))
            )
            logger.warn("Legger til behov ${OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver}")
            secureLogger.info("SuccessPacket=${packet.toJson()}")
        }

        return true
    }

}