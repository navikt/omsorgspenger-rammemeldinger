package no.nav.omsorgspenger.aleneom

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgspenger.Periode
import java.time.LocalDate
import java.util.*

internal fun Route.AleneOmOmsorgenApi(aleneOmOmsorgenService: AleneOmOmsorgenService) {
    post("/hentAleneOmOmsorgen") {
        val request = call.receive<HentAleneOmOmsorgenRequest>()
        val correlationId = call.request.header(HttpHeaders.XCorrelationId) ?: UUID.randomUUID().toString()

        val aleneOmOmsorgen = aleneOmOmsorgenService.hentSpleisetAleneOmOmsorgen(
            identitetsnummer = request.identitetsnummer,
            periode = Periode(request.fom, request.tom),
            correlationId = correlationId
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = HentAleneOmOmsorgenResponse(
                aleneOmOmsorgen = aleneOmOmsorgen
            )
        )
    }
}

private data class HentAleneOmOmsorgenRequest(
    val identitetsnummer: String, 
    val fom: LocalDate,
    val tom: LocalDate
)

private data class HentAleneOmOmsorgenResponse(
    val aleneOmOmsorgen : List<SpleisetAleneOmOmsorgen>
)