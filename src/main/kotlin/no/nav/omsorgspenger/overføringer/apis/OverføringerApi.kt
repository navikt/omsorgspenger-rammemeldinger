package no.nav.omsorgspenger.overføringer.apis

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.k9.rapid.behov.Behovsformat.iso8601
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import org.json.JSONArray
import org.json.JSONObject

internal fun Route.OverføringerApi(
    overføringRepository: OverføringRepository) {

    post("/hent-overforinger") {
        val request = call.receive<HentOverføringerRequest>()

        if (request.status.size != 1 || request.status.first() != "Aktiv") {
            call.respondJson(
                json = """{"melding":"Støtter ikke status ${request.status}"}""",
                status = HttpStatusCode.BadRequest
            )
            return@post
        }

        val overføringer = overføringRepository.hentAktiveOverføringer(
            saksnummer = setOf(request.saksnummer)
        )[request.saksnummer]

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
private fun List<GjeldendeOverføringGitt>.gittSomJson() = JSONArray().also { array ->
    forEach { gitt -> array.put(gitt.gittSomJson()) }
}

private fun List<GjeldendeOverføringFått>.fåttSomJson() = JSONArray().also { array ->
    forEach { fått -> array.put(fått.fåttSomJson()) }
}

private fun GjeldendeOverføringFått.fåttSomJson() = somJson().also {
    it.put("fra", JSONObject("""{"saksnummer":"$fra"}"""))
}
private fun GjeldendeOverføringGitt.gittSomJson() = somJson().also {
    it.put("til", JSONObject("""{"saksnummer":"$til"}"""))
}
private fun GjeldendeOverføring.somJson() = JSONObject().also { root ->
    root.put("lovanvendelser", JSONObject(lovanvendelser!!.somJson()))
    root.put("antallDager", antallDager)
    root.put("gjennomført", gjennomført.iso8601())
    root.put("periode", "$periode")
    root.put("status", "Aktiv")
}

private data class HentOverføringerRequest(
    val saksnummer: Saksnummer,
    val status: Set<String>
)