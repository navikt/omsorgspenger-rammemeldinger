package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneService
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Grunnlag
import no.nav.omsorgspenger.overføringer.Vurderinger
import no.nav.omsorgspenger.overføringer.meldinger.*
import no.nav.omsorgspenger.overføringer.meldinger.HentFordelingGirMeldingerMelding.HentFordelingGirMeldinger
import no.nav.omsorgspenger.overføringer.meldinger.HentMidlertidigAleneVedtakMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentMidlertidigAleneVedtakMelding.HentMidlertidigAleneVedtak
import no.nav.omsorgspenger.overføringer.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentPersonopplysningerMelding.HentPersonopplysninger
import no.nav.omsorgspenger.overføringer.meldinger.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentUtvidetRettVedtakMelding.HentUtvidetRettVedtak
import no.nav.omsorgspenger.overføringer.meldinger.OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding.OverføreOmsorgsdagerBehandling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding.OverføreOmsorgsdager
import no.nav.omsorgspenger.overføringer.meldinger.leggTilLøsningPar
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService

import org.slf4j.LoggerFactory

internal class BehandleOverføringAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    private val fordelingService: FordelingService,
    private val utvidetRettService: UtvidetRettService,
    private val midlertidigAleneService: MidlertidigAleneService
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(BehandleOverføringAvOmsorgsdager::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.utenLøsningPåBehov(OverføreOmsorgsdagerBehandling)
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.warn("TODO: Lagre at packet med id $id er håndtert. https://github.com/navikt/omsorgspenger-rammemeldinger/issues/12")
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("BehandleOverføringAvOmsorgsdager")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)

        val behandling = Behandling()
        // TODO: Legge tilbake periode i behandling https://github.com/navikt/omsorgspenger-rammemeldinger/issues/13

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

        logger.info("hentMidlertidigAleneVedtak")
        val midlertidigAleneVedtak = midlertidigAleneService.hentMidlertidigAleneVedtak(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = overføreOmsorgsdager.overordnetPeriode
        )

        val grunnlag = Vurderinger.vurderGrunnlag(
            grunnlag = Grunnlag(
                overføreOmsorgsdager = overføreOmsorgsdager,
                utvidetRettVedtak = utvidetRettVedtak,
                fordelingGirMeldinger = fordelingGirMeldinger,
                midlertidigAleneVedtak = midlertidigAleneVedtak
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

        val berørteIdentitetsnummer = setOf(
            overføreOmsorgsdager.overførerFra,
            overføreOmsorgsdager.overførerTil
        )

        logger.info("${berørteIdentitetsnummer.size} berørte parter for overføringen.")

        logger.info("karakteristikker = ${behandling.karakteristikker()}")

        logger.info("legger til behov med løsninger [$HentFordelingGirMeldinger, $HentUtvidetRettVedtak, $HentMidlertidigAleneVedtak, $OverføreOmsorgsdagerBehandling]")
        logger.warn("Løsning på behov [$HentUtvidetRettVedtak,$HentMidlertidigAleneVedtak] bør flyttes til 'omsorgspenger-rammevedtak'")
        packet.leggTilBehovMedLøsninger(
            aktueltBehov = OverføreOmsorgsdager,
            behovMedLøsninger = arrayOf(
                HentFordelingGirMeldingerMelding.behovMedLøsning(fordelingGirMeldinger),
                HentUtvidetRettVedtakMelding.behovMedLøsning(utvidetRettVedtak),
                HentMidlertidigAleneVedtakMelding.behovMedLøsning(midlertidigAleneVedtak),
                OverføreOmsorgsdagerBehandlingMelding.behovMedLøsning(
                    OverføreOmsorgsdagerBehandlingMelding.HeleBehandling(
                        behandling = behandling,
                        overføringer = overføringer
                    )
                )
            )
        )

        val inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre = omsorgsdagerTilgjengeligForOverføring.inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre(
            ønsketOmsorgsdagerÅOverføre = overføreOmsorgsdager.omsorgsdagerÅOverføre
        )

        if (inneholderMinstEnPeriodeMedFærreDagerEnnØnsketOmsorgsdagerÅOverføre && behandling.inneholderIkkeVerifiserbareVedtakOmUtvidetRett()) {
            logger.info("Legger til løsning på behov [$OverføreOmsorgsdager]")
            packet.leggTilLøsningPar(
                OverføreOmsorgsdagerMelding.løsning(OverføreOmsorgsdagerMelding.Løsningen(
                    utfall = Utfall.GosysJournalføringsoppgave,
                    begrunnelser = listOf(),
                    fra = overføreOmsorgsdager.overførerFra,
                    til = overføreOmsorgsdager.overførerTil,
                    overføringer = emptyList(),
                    parter = emptySet()
                ))
            )
            logger.info("legger til behov [$OpprettGosysJournalføringsoppgaver]")
            packet.leggTilBehovEtter(
                aktueltBehov = OverføreOmsorgsdager,
                behov = arrayOf(
                    OpprettGosysJournalføringsoppgaverMelding.behov(
                        OpprettGosysJournalføringsoppgaverMelding.BehovInput(
                            identitetsnummer = overføreOmsorgsdager.overførerFra,
                            journalpostIder = overføreOmsorgsdager.journalpostIder
                        )
                    )
                )
            )
        } else {
            logger.info("legger til behov [$HentPersonopplysninger,$HentOmsorgspengerSaksnummer]")
            packet.leggTilBehov(
                aktueltBehov = OverføreOmsorgsdager,
                behov = arrayOf(
                    HentPersonopplysningerMelding.behov(
                        HentPersonopplysningerMelding.BehovInput(
                            identitetsnummer = berørteIdentitetsnummer
                        )
                    ),
                    HentOmsorgspengerSaksnummerMelding.behov(
                        HentOmsorgspengerSaksnummerMelding.BehovInput(
                            identitetsnummer = berørteIdentitetsnummer
                        )
                    )
                )
            )
        }

        return true
    }
}