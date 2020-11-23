package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.formidling.FormidlingService
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.formidling.Formidling.opprettMeldingsBestillinger
import no.nav.omsorgspenger.overføringer.meldinger.FerdigstillJournalføringForOmsorgspengerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding.HentPersonopplysninger
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding.OverføreOmsorgsdagerBehandling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding.OverføreOmsorgsdager
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.statistikk.StatistikkMelding
import no.nav.omsorgspenger.statistikk.StatistikkService
import no.nav.omsorgspenger.saksnummer.identitetsnummer
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.ZonedDateTime

internal class PubliserOverføringAvOmsorgsdager (
    rapidsConnection: RapidsConnection,
    private val formidlingService: FormidlingService,
    behovssekvensRepository: BehovssekvensRepository,
    private val statistikkService: StatistikkService
) : PersistentBehovssekvensPacketListener(
    steg = "PubliserOverføringAvOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(PubliserOverføringAvOmsorgsdager::class.java)) {
    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.harLøsningPåBehov(
                    OverføreOmsorgsdagerBehandling,
                    HentPersonopplysninger
                )
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
                OverføreOmsorgsdagerBehandlingMelding.validateLøsning(it)
                HentPersonopplysningerMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("PubliserOverføringAvOmsorgsdager for $id")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)
        val behandling = OverføreOmsorgsdagerBehandlingMelding.hentLøsning(packet)
        val personopplysninger = HentPersonopplysningerMelding.hentLøsning(packet).also {
            require(it.keys.containsAll(behandling.alleSaksnummerMapping.identitetsnummer())) {
                "Mangler personopplysninger for en eller fler av personene berørt av overføringen."
            }
        }

        val utfall = when {
            behandling.oppfyllerIkkeInngangsvilkår -> Utfall.Avslått
            behandling.ingenOverføringer -> Utfall.Avslått
            else -> Utfall.Gjennomført
        }

        packet.leggTilLøsningPar(
            OverføreOmsorgsdagerMelding.løsning(OverføreOmsorgsdagerMelding.Løsningen(
                utfall = utfall,
                gjeldendeOverføringer = behandling.gjeldendeOverføringer,
                personopplysninger = personopplysninger,
                alleSaksnummerMapping = behandling.alleSaksnummerMapping
            ))
        )

        packet.leggTilBehovEtter(
            aktueltBehov = OverføreOmsorgsdager,
            behov = arrayOf(
                FerdigstillJournalføringForOmsorgspengerMelding.behov(
                    FerdigstillJournalføringForOmsorgspengerMelding.BehovInput(
                        identitetsnummer = overføreOmsorgsdager.overførerFra,
                        journalpostIder = overføreOmsorgsdager.journalpostIder,
                        saksnummer = behandling.alleSaksnummerMapping.getValue(overføreOmsorgsdager.overførerFra)
                    )
                )
            )
        )

        opprettMeldingsBestillinger(
            behovssekvensId = id,
            personopplysninger = personopplysninger,
            overføreOmsorgsdager = overføreOmsorgsdager,
            behandling = behandling
        ).also { when {
            it.isEmpty() -> secureLogger.warn("Melding(er) må sendes manuelt. Packet=${packet.toJson()}")
            else -> formidlingService.sendMeldingsbestillinger(it)
        }}

        statistikkService.publiser(StatistikkMelding(
                saksnummer = behandling.alleSaksnummerMapping.getValue(overføreOmsorgsdager.overførerFra),
                behandlingId = id,
                mottattDato = overføreOmsorgsdager.mottaksdato,
                registrertDato = packet["@opprettet"].asText().let { ZonedDateTime.parse(it).toLocalDate() },
                funksjonellTid = overføreOmsorgsdager.mottatt.toOffsetDateTime(),
                behandlingType = "overføring",
                behandlingStatus = "gjennomført",
                aktorId = personopplysninger.getValue(overføreOmsorgsdager.overførerFra).aktørId,
                tekniskTid = OffsetDateTime.now()
        ))

        secureLogger.info("SuccessPacket=${packet.toJson()}")

        return true
    }
}