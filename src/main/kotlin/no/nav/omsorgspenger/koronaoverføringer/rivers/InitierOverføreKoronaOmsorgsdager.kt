package no.nav.omsorgspenger.koronaoverføringer.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.k9.rapid.river.utenLøsningPåBehov
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.koronaoverføringer.Perioder.erStøttetPeriode
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import org.slf4j.LoggerFactory

internal class InitierOverføreKoronaOmsorgsdager(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "InitierOverføreKoronaOmsorgsdager",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierOverføreKoronaOmsorgsdager::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(OverføreKoronaOmsorgsdagerMelding.OverføreKoronaOmsorgsdager)
                it.utenLøsningPåBehov(HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer)
                OverføreKoronaOmsorgsdagerMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val overføringen = OverføreKoronaOmsorgsdagerMelding.hentBehov(packet)

        if (overføringen.periode.erStøttetPeriode()) {
            // Hent saksnummer
        } else {
            // Gosysoppgave
        }

        return true
    }

}