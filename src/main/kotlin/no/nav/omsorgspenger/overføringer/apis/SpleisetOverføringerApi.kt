package no.nav.omsorgspenger.overføringer.apis

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.omsorgspenger.apis.HentRammemeldingerRequest
import no.nav.omsorgspenger.extensions.correlationId

internal fun Route.SpleisetOverføringerApi(
    spleisetOverføringerService: SpleisetOverføringerService
) {

    post("/hentOverfoeringer") {
        val request = call.receive<HentRammemeldingerRequest>()

        val spleisetOverføringer = spleisetOverføringerService.hentSpleisetOverføringer(
            identitetsnummer = request.identitetsnummer,
            periode = request.periode,
            correlationId = call.request.correlationId()
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = spleisetOverføringer
        )
    }
}