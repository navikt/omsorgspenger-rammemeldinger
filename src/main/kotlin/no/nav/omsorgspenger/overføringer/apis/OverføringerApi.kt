package no.nav.omsorgspenger.overføringer.apis

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal fun Route.OverføringApi(
    overføringRepository: OverføringRepository,
    saksnummerService: SaksnummerService) {

    post("/hent-overforinger") {
        val request = call.receive<HentOverføringerRequest>()

        if (request.status.size != 1 || request.status.first() != "Aktiv") {
            call.respondJson(
                json = """{"melding":"Støtter ikke status ${request.status}"}""",
                status = HttpStatusCode.BadRequest
            )
            return@post
        }

        val saksnummer = saksnummerService.hentSaksnummer(
            identitetsnummer = request.identitetsnummer
        )

        if (saksnummer == null) {
            call.respondJson(
                json = """{"melding":"Fant ikke saksnummer på person."}""",
                status = HttpStatusCode.NotFound
            )
            return@post
        }

        val overføringer = overføringRepository.hentAktiveOverføringer(
            saksnummer = setOf(saksnummer)
        )[saksnummer]

        // TODO: Legg til tilgangsstyring

        call.respondJson(json = somJson(overføringer))
    }
}

private suspend fun ApplicationCall.respondJson(
    json: String,
    status: HttpStatusCode = HttpStatusCode.OK) = respondText(
    status = status,
    contentType = ContentType.Application.Json,
    text = json
)

private fun somJson(gjeldendeOverføringer: GjeldendeOverføringer?) = when (gjeldendeOverføringer) {
    null -> """{"fått":[],"gitt":[]}"""
    else -> JSONObject().also { root ->
        root.put("fått", gjeldendeOverføringer.fått.fåttSomJson())
        root.put("gitt", gjeldendeOverføringer.gitt.gittSomJson())
    }.toString()
}
private fun List<GjeldendeOverføringGitt>.gittSomJson() = JSONArray()
private fun List<GjeldendeOverføringFått>.fåttSomJson() = JSONArray()

private data class HentOverføringerRequest(
    val identitetsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: Set<String>
)