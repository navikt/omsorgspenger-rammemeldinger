package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.harLøsningPåBehov
import no.nav.k9.rapid.river.leggTilBehovEtter
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.formidling.FormidlingService
import no.nav.omsorgspenger.overføringer.OverføringLogg.gjennomførtOverføring
import no.nav.omsorgspenger.overføringer.Utfall
import no.nav.omsorgspenger.overføringer.formidling.Formidling.opprettMeldingsBestillinger
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding.OverføreOmsorgsdagerBehandling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding.OverføreOmsorgsdager
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerPersonopplysningerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerPersonopplysningerMelding.HentPersonopplysninger
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.FerdigstillJournalføringForOmsorgspengerMelding
import no.nav.omsorgspenger.rivers.meldinger.SendMeldingerManueltMelding
import no.nav.omsorgspenger.saksnummer.identitetsnummer
import org.slf4j.LoggerFactory

internal class PubliserOverføringAvOmsorgsdager (
    rapidsConnection: RapidsConnection,
    private val formidlingService: FormidlingService,
    behovssekvensRepository: BehovssekvensRepository,
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
                OverføreOmsorgsdagerPersonopplysningerMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("PubliserOverføringAvOmsorgsdager for $id")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)
        val behandling = OverføreOmsorgsdagerBehandlingMelding.hentLøsning(packet)
        val personopplysninger = OverføreOmsorgsdagerPersonopplysningerMelding.hentLøsning(packet).personopplysninger.also {
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

        val behovEtter = mutableListOf(
            FerdigstillJournalføringForOmsorgspengerMelding.behov(
                FerdigstillJournalføringForOmsorgspengerMelding.BehovInput(
                    identitetsnummer = overføreOmsorgsdager.overførerFra,
                    journalpostIder = overføreOmsorgsdager.journalpostIder,
                    saksnummer = behandling.alleSaksnummerMapping.getValue(overføreOmsorgsdager.overførerFra)
                )
            )
        )

        val meldingsbestillingerSendt = opprettMeldingsBestillinger(
            behovssekvensId = id,
            personopplysninger = personopplysninger,
            overføreOmsorgsdager = overføreOmsorgsdager,
            behandling = behandling
        ).let { when {
            it.isEmpty() -> {
                behovEtter.add(SendMeldingerManueltMelding.behov(OverføreOmsorgsdager))
                secureLogger.warn("Melding(er) må sendes manuelt.").let { false }
            }
            else -> formidlingService.sendMeldingsbestillinger(it).let { true }
        }}

        secureLogger.gjennomførtOverføring(
            fra = overføreOmsorgsdager.overførerFra,
            til = overføreOmsorgsdager.overførerTil,
            type = "ordinær",
            meldingsbestillingerSendt = meldingsbestillingerSendt
        )

        packet.leggTilBehovEtter(
            aktueltBehov = OverføreOmsorgsdager,
            behov = behovEtter.toTypedArray()
        )

        secureLogger.info("SuccessPacket=${packet.toJson()}")

        return true
    }
}