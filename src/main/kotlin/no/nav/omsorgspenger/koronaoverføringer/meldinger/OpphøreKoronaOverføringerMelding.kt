package no.nav.omsorgspenger.koronaoverføringer.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.overføringer.db.OpphørOverføringer
import no.nav.omsorgspenger.rivers.HentBehov
import no.nav.omsorgspenger.rivers.LeggTilLøsning
import java.time.LocalDate

internal object OpphøreKoronaOverføringerMelding :
    HentBehov<OpphøreKoronaOverføringerMelding.Behovet>,
    LeggTilLøsning<OpphørOverføringer.OpphørteOverføringer> {
    internal const val OpphøreKoronaOverføringer = "OpphøreKoronaOverføringer"

    internal data class Behovet(
        internal val fra: Saksnummer,
        internal val til: Saksnummer,
        internal val fraOgMed: LocalDate
    )

    override fun validateBehov(packet: JsonMessage) {
        packet.requireValue(BehovKeys.Versjon, "1.0.0")
        packet.interestedIn(
            BehovKeys.FraSaksnummer,
            BehovKeys.TilSaksnummer,
            BehovKeys.FraOgMed
        )
    }

    override fun hentBehov(packet: JsonMessage) = Behovet(
        fra = packet[BehovKeys.FraSaksnummer].asText(),
        til = packet[BehovKeys.TilSaksnummer].asText(),
        fraOgMed = packet[BehovKeys.FraOgMed].asLocalDate()
    )

    private object BehovKeys {
        val Versjon = "@behov.${OpphøreKoronaOverføringer}.versjon"
        val FraSaksnummer = "@behov.${OpphøreKoronaOverføringer}.fra.saksnummer"
        val TilSaksnummer = "@behov.${OpphøreKoronaOverføringer}.til.saksnummer"
        val FraOgMed = "@behov.${OpphøreKoronaOverføringer}.fraOgMed"
    }

    override fun løsning(løsning: OpphørOverføringer.OpphørteOverføringer): Pair<String, Map<String, *>> {
        return OpphøreKoronaOverføringer to mapOf(
            "deaktiverte" to løsning.deaktiverte,
            "nyTilOgMed" to løsning.nyTilOgMed
        )
    }
}