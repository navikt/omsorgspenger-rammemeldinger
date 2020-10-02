package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.fordelinger.somLøsning
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Grunnlag
import no.nav.omsorgspenger.overføringer.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.overføringer.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.overføringer.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.Vurderinger
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import no.nav.omsorgspenger.utvidetrett.somLøsning

import org.slf4j.LoggerFactory

internal class BehandleOverføringAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    private val fordelingService: FordelingService,
    private val utvidetRettService: UtvidetRettService
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(BehandleOverføringAvOmsorgsdager::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdagerMelding.Navn)
                it.utenLøsningPåBehov(OverføreOmsorgsdagerBehandlingMelding.Navn)
            }
            validate {
                OverføreOmsorgsdagerMelding(it).validate()
            }
        }.register(this)
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.warn("TODO: Lagre at packet med id $id er håndtert.")
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("BehandleOverføringAvOmsorgsdager")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding(packet).innhold()

        val behandling = Behandling()

        logger.info("hentFordelingGirMeldinger")
        val fordelingGirMeldinger = fordelingService.hentFordelingGirMeldinger(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = overføreOmsorgsdager.overordnetPeriode
        )

        logger.info("hentUtvidetRettVedtak")
        val utvidetRettVedtak = utvidetRettService.hentUtvidetRettVedtak(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = overføreOmsorgsdager.overordnetPeriode
        )

        val grunnlag = Vurderinger.vurderGrunnlag(
            grunnlag = Grunnlag(
                overføreOmsorgsdager = OverføreOmsorgsdagerMelding(packet).innhold(),
                utvidetRettVedtak = utvidetRettVedtak,
                fordelingGirMeldinger = fordelingGirMeldinger
            ),
            behandling = behandling
        )

        logger.info("vurderInngangsvilkår")
        Vurderinger.vurderInngangsvilkår(
            grunnlag = grunnlag,
            behandling = behandling
        )

        logger.info("beregnOmsorgsdagerTilgjengeligForOverføring")
        val omsorgsdagerTilgjengeligForOverføring = Beregninger.beregnOmsorgsdagerTilgjengeligForOverføring(
            grunnlag = grunnlag,
            behandling = behandling
        )

        logger.info("genererer overføringer")
        val overføringer = omsorgsdagerTilgjengeligForOverføring.somOverføringer(
            ønsketOmsorgsdagerÅOverføre = overføreOmsorgsdager.omsorgsdagerÅOverføre
        )

        logger.info("karakteristikker = ${behandling.karakteristikker()}")

        logger.info("legger til behov med løsninger [${HentFordelingGirMeldingerMelding.Navn}, ${HentUtvidetRettVedtakMelding.Navn}, ${OverføreOmsorgsdagerBehandlingMelding.Navn}]")
        logger.warn("Løsning på behovet ${HentUtvidetRettVedtakMelding.Navn} bør flyttes til 'omsorgspenger-rammevedtak'")
        packet.leggTilBehovMedLøsninger(
            aktueltBehov = OverføreOmsorgsdagerMelding.Navn,
            behovMedLøsninger = arrayOf(
                Behov(
                    navn = HentFordelingGirMeldingerMelding.Navn
                ) to fordelingGirMeldinger.somLøsning(),
                Behov(
                    navn = HentUtvidetRettVedtakMelding.Navn
                ) to utvidetRettVedtak.somLøsning(),
                Behov(
                    navn = OverføreOmsorgsdagerBehandlingMelding.Navn,
                ) to behandling.somLøsning(
                    nyeOverføringer = overføringer
                )
            )
        )

        val inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre = omsorgsdagerTilgjengeligForOverføring.inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre(
            ønsketOmsorgsdagerÅOverføre = overføreOmsorgsdager.omsorgsdagerÅOverføre
        )

        if (inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre && behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett()) {
            logger.info("legger til behov [${OpprettGosysJournalføringsoppgaverMelding.Navn}]")
            packet.leggTilBehov(
                aktueltBehov = OverføreOmsorgsdagerMelding.Navn,
                behov = arrayOf(Behov(
                    navn = OpprettGosysJournalføringsoppgaverMelding.Navn,
                    input = OpprettGosysJournalføringsoppgaverMelding.input(
                        journalpostIder = overføreOmsorgsdager.journalpostIder
                    )
                ))
            )
            logger.info("Legger til løsning på behov [${OverføreOmsorgsdagerMelding.Navn}]")
            packet.leggTilLøsning(
                behov = OverføreOmsorgsdagerMelding.Navn,
                løsning = MockLøsning.mockLøsning(
                    utfall = Utfall.GosysJournalføringsoppgave,
                    begrunnelser = listOf(),
                    fra = overføreOmsorgsdager.overførerFra,
                    til = overføreOmsorgsdager.overførerTil,
                    overføringer = emptyList(),
                    parter = emptySet()
                )
            )
        } else {
            logger.info("legger til behov [${HentPersonopplysningerMelding.Navn}]")
            packet.leggTilBehov(
                aktueltBehov = OverføreOmsorgsdagerMelding.Navn,
                behov = arrayOf(Behov(
                    navn = HentPersonopplysningerMelding.Navn,
                    input = HentPersonopplysningerMelding.input(
                        identitetsnummer = setOf(
                            overføreOmsorgsdager.overførerFra,
                            overføreOmsorgsdager.overførerTil
                        )
                    )
                ))
            )
        }

        return true
    }
}