package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.fordelinger.somLøsning
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneService
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.Grunnlag
import no.nav.omsorgspenger.overføringer.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.overføringer.Vurderinger
import no.nav.omsorgspenger.overføringer.meldinger.HentMidlertidigAleneVedtakMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentMidlertidigAleneVedtakMelding.HentMidlertidigAleneVedtak
import no.nav.omsorgspenger.overføringer.meldinger.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentUtvidetRettVedtakMelding.HentUtvidetRettVedtak
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
                it.utenLøsningPåBehov(OverføreOmsorgsdagerBehandlingMelding.Navn)
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.warn("TODO: Lagre at packet med id $id er håndtert.")
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("BehandleOverføringAvOmsorgsdager")

        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)

        val behandling = Behandling()
        // TODO: Legge tilbake periode i behandling?

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

        logger.info("legger til behov med løsninger [${HentFordelingGirMeldingerMelding.Navn}, $HentUtvidetRettVedtak, $HentMidlertidigAleneVedtak, ${OverføreOmsorgsdagerBehandlingMelding.Navn}]")
        logger.warn("Løsning på behov [$HentUtvidetRettVedtak,$HentMidlertidigAleneVedtak] bør flyttes til 'omsorgspenger-rammevedtak'")
        packet.leggTilBehovMedLøsninger(
            aktueltBehov = OverføreOmsorgsdager,
            behovMedLøsninger = arrayOf(
                HentMidlertidigAleneVedtakMelding.behovMedLøsning(midlertidigAleneVedtak),
                HentUtvidetRettVedtakMelding.behovMedLøsning(utvidetRettVedtak),
                Behov(
                    navn = HentFordelingGirMeldingerMelding.Navn
                ) to fordelingGirMeldinger.somLøsning(),
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
                aktueltBehov = OverføreOmsorgsdager,
                behov = arrayOf(Behov(
                    navn = OpprettGosysJournalføringsoppgaverMelding.Navn,
                    input = OpprettGosysJournalføringsoppgaverMelding.input(
                        journalpostIder = overføreOmsorgsdager.journalpostIder
                    )
                ))
            )
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
        } else {
            logger.info("legger til behov [${HentPersonopplysningerMelding.Navn},${HentOmsorgspengerSaksnummerMelding.Navn}]")
            packet.leggTilBehov(
                aktueltBehov = OverføreOmsorgsdager,
                behov = arrayOf(
                    Behov(
                        navn = HentPersonopplysningerMelding.Navn,
                        input = HentPersonopplysningerMelding.input(
                            identitetsnummer = berørteIdentitetsnummer
                        )
                    ),
                    Behov(
                        navn = HentOmsorgspengerSaksnummerMelding.Navn,
                        input = HentOmsorgspengerSaksnummerMelding.input(
                            identitetsnummer = berørteIdentitetsnummer
                        )
                    )
                )
            )
        }

        return true
    }
}