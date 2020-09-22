package no.nav.omsorgspenger.overføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.fordelinger.somLøsning
import no.nav.omsorgspenger.overføringer.HentFordelingGirMeldingerMelding
import no.nav.omsorgspenger.overføringer.HentOmsorgspengerSaksnummerMelding
import no.nav.omsorgspenger.overføringer.HentUtvidetRettVedtakMelding
import no.nav.omsorgspenger.overføringer.OverføreOmsorgsdagerMelding
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import no.nav.omsorgspenger.utvidetrett.somLøsning

import org.slf4j.LoggerFactory

internal class StartOverføringAvOmsorgsdager(
    rapidsConnection: RapidsConnection,
    private val fordelingService: FordelingService,
    private val utvidetRettService: UtvidetRettService
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreOmsorgsdagerMelding.Navn)
                it.utenLøsningPåBehov(
                    HentOmsorgspengerSaksnummerMelding.Navn,
                    HentUtvidetRettVedtakMelding.Navn,
                    HentFordelingGirMeldingerMelding.Navn
                )
            }
            validate {
                OverføreOmsorgsdagerMelding(it).validate()
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val id = packet["@id"].asText()
        val overføreOmsorgsdager = OverføreOmsorgsdagerMelding(packet)

        logger.info("$id -> StartOverføringAvOmsorgsdager")


        overføreOmsorgsdager.barn().any { it.utvidetRett }
        logger.info("Henter 'FordelingGirMeldinger'")
        val fordelingGirMeldinger = fordelingService.hentFordelingGirMeldinger(
            identitetsnummer = overføreOmsorgsdager.overførerFra(),
            periode = overføreOmsorgsdager.overordnetPeriode()
        )

        logger.info("Henter 'UtvidetRettVedtak'")
        val utvidetRettVedtak = utvidetRettService.hentUtvidetRettVedtak(
            identitetsnummer = overføreOmsorgsdager.overførerFra(),
            periode = overføreOmsorgsdager.overordnetPeriode()
        )

        // TODO: Verifisere UtvidetRett
        // Legg til  behov FerdigstillJournalføringOmsorgspenger

        val input = mapOf(
            "identitetsnummer" to overføreOmsorgsdager.overførerFra(),
            "periode" to overføreOmsorgsdager.overordnetPeriode().toString()

        )

        packet.leggTilBehovMedLøsninger(
            OverføreOmsorgsdagerMelding.Navn,
            Pair(Behov(navn = HentFordelingGirMeldingerMelding.Navn, input = input), fordelingGirMeldinger.somLøsning()),
            Pair(Behov(navn = HentUtvidetRettVedtakMelding.Navn, input = input), utvidetRettVedtak.somLøsning())
        )

        logger.info("Legger til behov for '${HentOmsorgspengerSaksnummerMelding.Navn}'")
        packet.leggTilBehov(
            OverføreOmsorgsdagerMelding.Navn,
            Behov(navn = HentOmsorgspengerSaksnummerMelding.Navn, input = input)
        )

        context.sendMedId(packet)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(StartOverføringAvOmsorgsdager::class.java)
    }
}