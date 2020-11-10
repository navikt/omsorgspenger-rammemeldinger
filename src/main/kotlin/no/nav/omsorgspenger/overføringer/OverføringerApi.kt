package no.nav.omsorgspenger.overføringer

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.correlationId
import java.time.LocalDate

internal fun Route.OverføringerApi(
    overføringService: OverføringService) {

    post("/hentOverfoeringer") {
        val request = call.receive<HentOverføringerRequest>()

        val spleisetOverføringer = overføringService.hentSpleisetOverføringer(
            identitetsnummer = request.identitetsnummer,
            periode = Periode(
                fom = request.fom,
                tom = request.tom
            ),
            correlationId = call.request.correlationId()
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = spleisetOverføringer
        )
    }
}


private data class HentOverføringerRequest(
    val identitetsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate
)