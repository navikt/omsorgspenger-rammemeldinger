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
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerPersonopplysningerMelding.fellesEnhet
import no.nav.omsorgspenger.overføringer.OverføringLogg.gjennomførtOverføring
import no.nav.omsorgspenger.overføringer.Utfall
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.FerdigstillJournalføringForOmsorgspengerMelding
import no.nav.omsorgspenger.rivers.meldinger.SendMeldingManueltMelding
import no.nav.omsorgspenger.statistikk.StatistikkMelding
import no.nav.omsorgspenger.statistikk.StatistikkService
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class PubliserOverføreKoronaOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository,
    private val formidlingService: FormidlingService,
    private val statistikkService: StatistikkService
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
        val utfall = when (behandling.gjennomførtOverføringer) {
            true -> Utfall.Gjennomført
            false -> Utfall.Avslått
        }

        packet.leggTilLøsningPar(OverføreKoronaOmsorgsdagerMelding.løsning(
            OverføreKoronaOmsorgsdagerMelding.Løsningen(
                utfall = utfall,
                gjeldendeOverføringer = behandling.gjeldendeOverføringer,
                alleSaksnummerMapping = behandling.alleSaksnummerMapping,
                personopplysninger = personopplysninger
            )
        ))

        val behovEtter = mutableListOf(FerdigstillJournalføringForOmsorgspengerMelding.behov(
            FerdigstillJournalføringForOmsorgspengerMelding.BehovInput(
                identitetsnummer = behovet.fra,
                journalpostIder = behovet.journalpostIder,
                saksnummer = behandling.fraSaksnummer
            )
        ))

        val meldingsbestillingerSendt = Formidling.opprettMeldingsbestillinger(
            behovssekvensId = id,
            personopplysninger = personopplysninger,
            behovet = behovet,
            behandling = behandling
        ).let { when {
            it.isEmpty() -> {
                secureLogger.warn("Melding(er) må sendes manuelt.").let { false }
                behovEtter.add(SendMeldingManueltMelding.behov(OverføreKoronaOmsorgsdagerMelding.OverføreKoronaOmsorgsdager))
            }
            else -> formidlingService.sendMeldingsbestillinger(it).let { true }
        }}

        secureLogger.gjennomførtOverføring(
            fra = behovet.fra,
            til = behovet.til,
            type = "korona",
            meldingsbestillingerSendt = meldingsbestillingerSendt
        )

        packet.leggTilBehovEtter(
            aktueltBehov = aktueltBehov,
            behov = behovEtter.toTypedArray()
        )

        val fellesEnhet = personopplysninger
            .filterKeys { it == behovet.fra || it == behovet.til }
            .fellesEnhet(behovet.fra)

        statistikkService.publiser(StatistikkMelding.instance(
            enhet = fellesEnhet,
            aktørId = personopplysninger.getValue(behovet.fra).aktørId,
            saksnummer = behandling.alleSaksnummerMapping.getValue(behovet.fra),
            behovssekvensId = id,
            mottatt = behovet.mottatt,
            mottaksdato = behovet.mottaksdato,
            registreringsdato = packet["@opprettet"].asText().let { ZonedDateTime.parse(it).toLocalDate() },
            undertype = "koronaoverføring",
            behandlingType = "søknad",
            behandlingResultat = when (utfall) {
                Utfall.Avslått -> "avslått"
                Utfall.Gjennomført -> "gjennomført"
                Utfall.GosysJournalføringsoppgaver -> throw IllegalStateException("Uventet utfall: $utfall")
            }
        ))
        secureLogger.info("SuccessPacket=${packet.toJson()}")

        return true
    }
}