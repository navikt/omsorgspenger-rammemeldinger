package no.nav.omsorgspenger.aleneom.apis

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgspenger.ApiTyper
import no.nav.omsorgspenger.extensions.correlationId

internal fun Route.SpleisetAleneOmOmsorgenApi(
    spleisetAleneOmOmsorgenService: SpleisetAleneOmOmsorgenService) {

    post("/hentAleneOmOmsorgen") {
        val request = call.receive<ApiTyper.HentRammemeldingerRequest>()

        val aleneOmOmsorgen = spleisetAleneOmOmsorgenService.hentSpleisetAleneOmOmsorgen(
            identitetsnummer = request.identitetsnummer,
            periode = request.periode,
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

private data class HentAleneOmOmsorgenResponse(
    val aleneOmOmsorgen : List<SpleisetAleneOmOmsorgen>
)