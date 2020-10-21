package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behovsformat

typealias AktørId = String
typealias Identitetsnummer = String
typealias Saksnummer = String
typealias JournalpostId = String
typealias CorrelationId = String
typealias BehovssekvensId = String
internal fun JsonMessage.correlationId() : CorrelationId = get(Behovsformat.CorrelationId).asText()

internal data class Kilde(
    val id: String,
    val type: String
)

internal data class Saksreferanse(
    val saksnummer: Saksnummer,
    val identitetsnummer: Identitetsnummer
)
