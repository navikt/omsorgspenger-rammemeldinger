package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.correlationId
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.fordelinger.meldinger.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.fordelinger.meldinger.HentFordelingGirMeldingerMelding.HentFordelingGirMeldinger
import no.nav.omsorgspenger.koronaoverføringer.ManuellVurdering
import no.nav.omsorgspenger.koronaoverføringer.Perioder
import no.nav.omsorgspenger.koronaoverføringer.Perioder.erStøttetPeriode
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import no.nav.omsorgspenger.utvidetrett.meldinger.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.utvidetrett.meldinger.HentUtvidetRettVedtakMelding.HentUtvidetRettVedtak
import org.slf4j.LoggerFactory

internal class InitierOverføreKoronaOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository,
    private val fordelingService: FordelingService,
    private val utvidetRettService: UtvidetRettService,
    private val enableBehandling: Boolean
) : PersistentBehovssekvensPacketListener(
    steg = "InitierOverføreKoronaOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierOverføreKoronaOmsorgsdager::class.java)) {

    private val aktueltBehov = OverføreKoronaOmsorgsdagerMelding.OverføreKoronaOmsorgsdager

    init {
        logger.info("EnableBehandling=$enableBehandling")
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(aktueltBehov)
                it.utenLøsningPåBehov(HentOmsorgspengerSaksnummer)
                OverføreKoronaOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun doHandlePacket(id: String, packet: JsonMessage): Boolean {
        val perioden = OverføreKoronaOmsorgsdagerMelding.hentBehov(packet).periode
        logger.info("Vurderer videre steg for søknad for perioden $perioden")
        return when (perioden.erStøttetPeriode()) {
            true -> when (enableBehandling) {
                true -> super.doHandlePacket(id, packet)
                false -> logger.warn("Behandling av koronaoverføringer er ikke skrudd på").let { false }
            }
            false -> super.doHandlePacket(id, packet)
        }
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behovet = OverføreKoronaOmsorgsdagerMelding.hentBehov(packet)

        if (ManuellVurdering.måVurderesManuelt(behovet)) {
            packet.leggTilLøsningPar(OverføreKoronaOmsorgsdagerMelding.løsning(
                OverføreKoronaOmsorgsdagerMelding.Løsningen()
            ))
            packet.leggTilBehovEtter(
                aktueltBehov = aktueltBehov,
                behov = arrayOf(behovet.somOpprettGosysJournalføringsoppgaverBehov())
            )
            logger.info("Legger til behov $OpprettGosysJournalføringsoppgaver")
            secureLogger.info("SuccessPacket=${packet.toJson()}")
        } else {
            require(enableBehandling) {
                "Behandling av koronaoverføringer er ikke skrudd på."
            }

            val identitetsnummer = behovet.fra
            val correlationId = packet.correlationId()
            val periode = Perioder.behandlingsPeriode(
                mottaksdato = behovet.mottaksdato,
                periode = behovet.periode
            )

            logger.info("Henter rammemeldinger & rammevedtak")
            val fordelingGirMeldinger = fordelingService.hentFordelingGirMeldinger(
                identitetsnummer = identitetsnummer,
                periode = periode,
                correlationId = correlationId
            )

            val utvidetRettVedtak = utvidetRettService.hentUtvidetRettVedtak(
                identitetsnummer = identitetsnummer,
                periode = periode,
                correlationId = correlationId
            )

            logger.info("legger til behov med løsninger [${HentFordelingGirMeldinger}, ${HentUtvidetRettVedtak}]")
            logger.warn("Løsning på behov [${HentUtvidetRettVedtak}] bør flyttes til 'omsorgspenger-rammevedtak'")
            val inputHentingAvRammer = mapOf(
                "periode" to "$periode",
                "identitetsnummer" to identitetsnummer
            )
            packet.leggTilBehovMedLøsninger(
                aktueltBehov = aktueltBehov,
                behovMedLøsninger = arrayOf(
                    HentFordelingGirMeldingerMelding.behovMedLøsning(inputHentingAvRammer, fordelingGirMeldinger),
                    HentUtvidetRettVedtakMelding.behovMedLøsning(inputHentingAvRammer, utvidetRettVedtak)
                )
            )

            logger.info("Legger til behov $HentOmsorgspengerSaksnummer")
            // TODO: Her skal vi også legge til behov for `VurderRelasjoenr`
            packet.leggTilBehov(
                aktueltBehov = aktueltBehov,
                behov = arrayOf(HentOmsorgspengerSaksnummerMelding.behov(
                    HentOmsorgspengerSaksnummerMelding.BehovInput(
                        identitetsnummer = setOf(behovet.fra, behovet.til)
                    )
                ))
            )
        }
        return true
    }

}