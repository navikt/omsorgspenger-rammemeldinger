package no.nav.omsorgspenger.extensions

import io.ktor.http.*
import io.ktor.server.request.*
import no.nav.omsorgspenger.CorrelationId
import java.util.*

internal fun ApplicationRequest.correlationId(): CorrelationId =
    this.headers[HttpHeaders.XCorrelationId] ?: UUID.randomUUID().toString()