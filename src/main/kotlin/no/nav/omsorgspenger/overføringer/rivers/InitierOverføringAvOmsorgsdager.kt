package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.correlationId
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.midlertidigalene.MidlertidigAleneService
import no.nav.omsorgspenger.overføringer.meldinger.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentFordelingGirMeldingerMelding.HentFordelingGirMeldinger
import no.nav.omsorgspenger.overføringer.meldinger.HentMidlertidigAleneVedtakMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentMidlertidigAleneVedtakMelding.HentMidlertidigAleneVedtak
import no.nav.omsorgspenger.overføringer.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.overføringer.meldinger.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentUtvidetRettVedtakMelding.HentUtvidetRettVedtak
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding.OverføreOmsorgsdagerBehandling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding.OverføreOmsorgsdager
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import org.slf4j.LoggerFactory

internal class InitierOverføringAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    private val fordelingService: FordelingService,
    private val utvidetRettService: UtvidetRettService,
    private val midlertidigAleneService: MidlertidigAleneService
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(InitierOverføringAvOmsorgsdager::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdager)
                it.utenLøsningPåBehov(
                    HentOmsorgspengerSaksnummer,
                    HentFordelingGirMeldinger,
                    HentUtvidetRettVedtak,
                    HentMidlertidigAleneVedtak,
                    OverføreOmsorgsdagerBehandling
                )
            }
            validate {
                OverføreOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("InitierOverføringAvOmsorgsdager for $id")
        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding.hentBehov(packet)
        val periode = overføreOmsorgsdager.overordnetPeriode
        val correlationId = packet.correlationId()

        logger.info("Henter rammemeldinger & rammevedtak")
        val fordelingGirMeldinger = fordelingService.hentFordelingGirMeldinger(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = periode,
            correlationId = correlationId
        )

        val utvidetRettVedtak = utvidetRettService.hentUtvidetRettVedtak(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = periode,
            correlationId = correlationId
        )

        val midlertidigAleneVedtak = midlertidigAleneService.hentMidlertidigAleneVedtak(
            identitetsnummer = overføreOmsorgsdager.overførerFra,
            periode = periode,
            correlationId = correlationId
        )

        logger.info("legger til behov med løsninger [$HentFordelingGirMeldinger, $HentUtvidetRettVedtak, $HentMidlertidigAleneVedtak]")
        logger.warn("Løsning på behov [$HentUtvidetRettVedtak,$HentMidlertidigAleneVedtak] bør flyttes til 'omsorgspenger-rammevedtak'")
        packet.leggTilBehovMedLøsninger(
            aktueltBehov = OverføreOmsorgsdager,
            // TODO: input på meldingene..
            behovMedLøsninger = arrayOf(
                HentFordelingGirMeldingerMelding.behovMedLøsning(fordelingGirMeldinger),
                HentUtvidetRettVedtakMelding.behovMedLøsning(utvidetRettVedtak),
                HentMidlertidigAleneVedtakMelding.behovMedLøsning(midlertidigAleneVedtak)
            )
        )

        logger.info("legger til behov [$HentOmsorgspengerSaksnummer]")
        packet.leggTilBehov(
            aktueltBehov = OverføreOmsorgsdager,
            behov = arrayOf(
                HentOmsorgspengerSaksnummerMelding.behov(
                    HentOmsorgspengerSaksnummerMelding.BehovInput(
                        identitetsnummer = setOf(
                            overføreOmsorgsdager.overførerFra,
                            overføreOmsorgsdager.overførerTil
                        )
                    )
                )
            )
        )
        return true
    }
}