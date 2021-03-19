package no.nav.omsorgspenger.koronaoverføringer.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.rivers.HentBehov
import java.time.LocalDate

internal object OpphørekoronaOverføringerMelding : HentBehov<OpphørekoronaOverføringerMelding.Behovet>{

    internal data class Behovet(
        internal val fra: Saksnummer,
        internal val til: Saksnummer,
        internal val fraOgMed: LocalDate
    )

    override fun validateBehov(packet: JsonMessage) {
        TODO("Not yet implemented")
    }

    override fun hentBehov(packet: JsonMessage): Behovet {
        TODO("Not yet implemented")
    }
}