package no.nav.omsorgspenger.koronaoverføringer.apis

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgspenger.ApiTyper
import no.nav.omsorgspenger.extensions.correlationId

internal fun Route.SpleisetKoronaOverføringerApi(
    spleisetKoronaOverføringerService: SpleisetKoronaOverføringerService) {

    post("/hent-korona-overforinger") {
        val request = call.receive<ApiTyper.HentRammemeldingerRequest>()

        val spleisetOverføringer = spleisetKoronaOverføringerService.hentSpleisetOverføringer(
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