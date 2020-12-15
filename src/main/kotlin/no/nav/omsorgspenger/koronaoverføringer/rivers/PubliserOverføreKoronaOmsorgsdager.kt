package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.harLøsningPåBehov
import no.nav.k9.rapid.river.leggTilBehovEtter
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.formidling.FormidlingService
import no.nav.omsorgspenger.koronaoverføringer.formidling.Formidling
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerPersonopplysningerMelding
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.FerdigstillJournalføringForOmsorgspengerMelding
import org.slf4j.LoggerFactory

internal class PubliserOverføreKoronaOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository,
    private val formidlingService: FormidlingService
) : PersistentBehovssekvensPacketListener(
    steg = "PubliserOverføreKoronaOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(PubliserOverføreKoronaOmsorgsdager::class.java)) {

    private val aktueltBehov = OverføreKoronaOmsorgsdagerMelding.OverføreKoronaOmsorgsdager

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(aktueltBehov)
                it.harLøsningPåBehov(
                    OverføreKoronaOmsorgsdagerBehandlingMelding.OverføreKoronaOmsorgsdagerBehandling,
                    OverføreKoronaOmsorgsdagerPersonopplysningerMelding.HentPersonopplysninger
                )
                OverføreKoronaOmsorgsdagerMelding.validateBehov(it)
                OverføreKoronaOmsorgsdagerBehandlingMelding.validateLøsning(it)
                OverføreKoronaOmsorgsdagerPersonopplysningerMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behovet = OverføreKoronaOmsorgsdagerMelding.hentBehov(packet)
        val behandling = OverføreKoronaOmsorgsdagerBehandlingMelding.hentLøsning(packet)
        val personopplysninger = OverføreKoronaOmsorgsdagerPersonopplysningerMelding.hentLøsning(packet).personopplysninger

        packet.leggTilLøsningPar(OverføreKoronaOmsorgsdagerMelding.løsning(
            OverføreKoronaOmsorgsdagerMelding.Løsningen()
        ))

        Formidling.opprettMeldingsbestillinger(
            behovssekvensId = id,
            personopplysninger = personopplysninger,
            behovet = behovet,
            behandling = behandling
        ).also { when {
            it.isEmpty() -> secureLogger.warn("Melding(er) må sendes manuelt.")
            else -> formidlingService.sendMeldingsbestillinger(it)
        }}

        packet.leggTilBehovEtter(
            aktueltBehov = aktueltBehov,
            behov = arrayOf(
                FerdigstillJournalføringForOmsorgspengerMelding.behov(
                    FerdigstillJournalføringForOmsorgspengerMelding.BehovInput(
                        identitetsnummer = behovet.fra,
                        journalpostIder = behovet.journalpostIder,
                        saksnummer = behandling.fraSaksnummer
                    )
                )
            )
        )

        secureLogger.info("SuccessPacket=${packet.toJson()}")

        return true
    }
}