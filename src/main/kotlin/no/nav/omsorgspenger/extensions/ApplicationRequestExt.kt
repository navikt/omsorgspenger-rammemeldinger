package no.nav.omsorgspenger.extensions

import io.ktor.http.*
import io.ktor.request.*
import no.nav.omsorgspenger.CorrelationId
import java.util.*

internal fun ApplicationRequest.correlationId() : CorrelationId =
    header(HttpHeaders.XCorrelationId) ?: UUID.randomUUID().toString()