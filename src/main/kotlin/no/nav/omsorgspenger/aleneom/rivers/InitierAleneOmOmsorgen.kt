package no.nav.omsorgspenger.aleneom.rivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.k9.rapid.river.utenLøsningPåBehov
import no.nav.omsorgspenger.aleneom.meldinger.AleneOmOmsorgenMelding
import no.nav.omsorgspenger.aleneom.meldinger.AleneOmOmsorgenPersonopplysningerMelding
import no.nav.omsorgspenger.behovssekvens.BehovssekvensRepository
import no.nav.omsorgspenger.behovssekvens.PersistentBehovssekvensPacketListener
import no.nav.omsorgspenger.personopplysninger.HentPersonopplysningerInput
import no.nav.omsorgspenger.rivers.meldinger.HentOmsorgspengerSaksnummerMelding
import org.slf4j.LoggerFactory

internal class InitierAleneOmOmsorgen(
    rapidsConnection: RapidsConnection,
    behovssekvensRepository: BehovssekvensRepository
) : PersistentBehovssekvensPacketListener(
    steg = "InitierAleneOmOmsorgen",
    behovssekvensRepository = behovssekvensRepository,
    logger = LoggerFactory.getLogger(InitierAleneOmOmsorgen::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(AleneOmOmsorgenMelding.AleneOmOmsorgen)
                it.utenLøsningPåBehov(
                    HentOmsorgspengerSaksnummerMelding.HentOmsorgspengerSaksnummer,
                    AleneOmOmsorgenPersonopplysningerMelding.HentPersonopplysninger
                )
                AleneOmOmsorgenMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behovet = AleneOmOmsorgenMelding.hentBehov(packet)

        packet.leggTilBehov(AleneOmOmsorgenMelding.AleneOmOmsorgen,
            HentOmsorgspengerSaksnummerMelding.behov(behovInput = HentOmsorgspengerSaksnummerMelding.BehovInput(
                identitetsnummer = setOf(behovet.identitetsnummer)
            )),
            AleneOmOmsorgenPersonopplysningerMelding.behov(behovInput = HentPersonopplysningerInput(
                identitetsnummer = behovet.barn.map { it.identitetsnummer }.toSet()
            ))
        )

        return true
    }
}
