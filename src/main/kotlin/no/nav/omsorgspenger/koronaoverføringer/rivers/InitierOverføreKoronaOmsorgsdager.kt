package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.correlationId
import no.nav.omsorgspenger.extensions.erFørEllerLik
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.fordelinger.meldinger.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.fordelinger.meldinger.HentFordelingGirMeldingerMelding.HentFordelingGirMeldinger
import no.nav.omsorgspenger.koronaoverføringer.ManuellVurdering
import no.nav.omsorgspenger.koronaoverføringer.Perioder
import no.nav.omsorgspenger.koronaoverføringer.apis.SpleisetKoronaOverføringerService
import no.nav.omsorgspenger.koronaoverføringer.meldinger.HentKoronaOverføringGirMeldingerMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.HentKoronaOverføringGirMeldingerMelding.HentKoronaOverføringGirMeldinger
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringerService
import no.nav.omsorgspenger.overføringer.meldinger.HentOverføringGirMeldingerMelding
import no.nav.omsorgspenger.overføringer.meldinger.HentOverføringGirMeldingerMelding.HentOverføringGirMeldinger
import no.nav.omsorgspenger.personopplysninger.VurderRelasjonerMelding
import no.nav.omsorgspenger.rivers.leggTilLøsningPar
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer
import no.nav.omsorgspenger.rivers.meldinger.OpprettGosysJournalføringsoppgaverMelding.OpprettGosysJournalføringsoppgaver
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import no.nav.omsorgspenger.utvidetrett.meldinger.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.utvidetrett.meldinger.HentUtvidetRettVedtakMelding.HentUtvidetRettVedtak
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class InitierOverføreKoronaOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository,
    private val fordelingService: FordelingService,
    private val utvidetRettService: UtvidetRettService,
    private val spleisetOverføringerService: SpleisetOverføringerService,
    private val spleisetKoronaOverføringerService: SpleisetKoronaOverføringerService,
    private val behandleMottattEtter: LocalDate
) : PersistentBehovssekvensPacketListener(
    steg = "InitierOverføreKoronaOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierOverføreKoronaOmsorgsdager::class.java)) {

    private val aktueltBehov = OverføreKoronaOmsorgsdagerMelding.OverføreKoronaOmsorgsdager

    init {
        logger.info("BehandleMottattEtter=$behandleMottattEtter")
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(aktueltBehov)
                it.utenLøsningPåBehov(HentOmsorgspengerSaksnummer)
                OverføreKoronaOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behovet = OverføreKoronaOmsorgsdagerMelding.hentBehov(packet)
        logger.info("Vurderer videre steg for søknad for perioden ${behovet.periode}")

        when {
            ManuellVurdering.måVurderesManuelt(behovet) -> {
                packet.leggTilLøsningPar(
                    OverføreKoronaOmsorgsdagerMelding.løsning(
                        OverføreKoronaOmsorgsdagerMelding.Løsningen.GosysJournalføringsoppgaver
                    )
                )
                packet.leggTilBehovEtter(
                    aktueltBehov = aktueltBehov,
                    behov = arrayOf(behovet.somOpprettGosysJournalføringsoppgaverBehov())
                )
                logger.info("Legger til behov $OpprettGosysJournalføringsoppgaver")
                secureLogger.info("SuccessPacket=${packet.toJson()}")
            }
            behovet.erBehandletIInfotrygd() -> {
                logger.warn("Er behandlet i Infotrygd.")
                packet.leggTilLøsning(
                    behov = aktueltBehov,
                    løsning = mapOf("melding" to "Er behandlet i Infotrygd.")
                )
            }
            else -> {
                require(behovet.skalBehandles())

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

                val overføringGirMeldinger = spleisetOverføringerService.hentSpleisetOverføringer(
                    identitetsnummer = identitetsnummer,
                    periode = periode,
                    correlationId = correlationId
                ).gitt

                val koronaoverføringGirMeldinger = spleisetKoronaOverføringerService.hentSpleisetOverføringer(
                    identitetsnummer = identitetsnummer,
                    periode = periode,
                    correlationId = correlationId
                ).gitt

                val utvidetRettVedtak = utvidetRettService.hentUtvidetRettVedtak(
                    identitetsnummer = identitetsnummer,
                    periode = periode,
                    correlationId = correlationId
                )

                logger.info("legger til behov med løsninger [${HentFordelingGirMeldinger}, ${HentUtvidetRettVedtak}, ${HentOverføringGirMeldinger}, ${HentKoronaOverføringGirMeldinger}]")
                logger.warn("Løsning på behov [${HentUtvidetRettVedtak}] bør flyttes til 'omsorgspenger-rammevedtak'")
                val inputHentingAvRammer = mapOf(
                    "periode" to "$periode",
                    "identitetsnummer" to identitetsnummer
                )
                packet.leggTilBehovMedLøsninger(
                    aktueltBehov = aktueltBehov,
                    behovMedLøsninger = arrayOf(
                        HentFordelingGirMeldingerMelding.behovMedLøsning(inputHentingAvRammer, fordelingGirMeldinger),
                        HentOverføringGirMeldingerMelding.behovMedLøsning(inputHentingAvRammer, overføringGirMeldinger),
                        HentKoronaOverføringGirMeldingerMelding.behovMedLøsning(inputHentingAvRammer, koronaoverføringGirMeldinger),
                        HentUtvidetRettVedtakMelding.behovMedLøsning(inputHentingAvRammer, utvidetRettVedtak)
                    )
                )

                logger.info("Legger til behov $HentOmsorgspengerSaksnummer & ${VurderRelasjonerMelding.VurderRelasjoner}")
                packet.leggTilBehov(
                    aktueltBehov = aktueltBehov,
                    behov = arrayOf(
                        HentOmsorgspengerSaksnummerMelding.behov(
                            HentOmsorgspengerSaksnummerMelding.BehovInput(
                                identitetsnummer = setOf(behovet.fra, behovet.til)
                            )
                        ),
                        VurderRelasjonerMelding.behov(
                            VurderRelasjonerMelding.BehovInput(
                                identitetsnummer = behovet.fra,
                                til = behovet.barn.map { it.identitetsnummer }.toSet()
                            )
                        )
                    )
                )
            }
        }
        return true
    }

    private fun OverføreKoronaOmsorgsdagerMelding.Behovet.erBehandletIInfotrygd() =
        mottaksdato.year == 2021 && mottaksdato.erFørEllerLik(behandleMottattEtter)
    private fun OverføreKoronaOmsorgsdagerMelding.Behovet.skalBehandles() =
        mottaksdato.isAfter(behandleMottattEtter)
}