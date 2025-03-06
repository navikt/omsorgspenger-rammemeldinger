package no.nav.omsorgspenger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behovsformat
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId

typealias AktørId = String
typealias Identitetsnummer = String
typealias Saksnummer = String
typealias JournalpostId = String
typealias CorrelationId = String
internal fun JsonMessage.correlationId() : CorrelationId = get(Behovsformat.CorrelationId).asText()

internal data class Kilde(
    val id: String,
    val type: String) {
    internal companion object {
        internal fun internKilde(
            behovssekvensId: BehovssekvensId,
            type: String) = Kilde(
            id = behovssekvensId,
            type = "OmsorgspengerRammemeldinger[$type]"
        )
    }
}