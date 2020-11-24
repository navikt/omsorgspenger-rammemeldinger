package no.nav.omsorgspenger.aleneom.apis

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.correlationId
import java.time.LocalDate

internal fun Route.SpleisetAleneOmOmsorgenApi(
    spleisetAleneOmOmsorgenService: SpleisetAleneOmOmsorgenService) {

    post("/hentAleneOmOmsorgen") {
        val request = call.receive<HentAleneOmOmsorgenRequest>()

        val aleneOmOmsorgen = spleisetAleneOmOmsorgenService.hentSpleisetAleneOmOmsorgen(
            identitetsnummer = request.identitetsnummer,
            periode = Periode(request.fom, request.tom),
            correlationId = call.request.correlationId()
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