package no.nav.omsorgspenger.overføringer.apis

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.k9.rapid.behov.Behovsformat.iso8601
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import org.json.JSONArray
import org.json.JSONObject

internal fun Route.OverføringerApi(
    overføringRepository: OverføringRepository,
    saksnummerService: SaksnummerService) {

    get("/overforinger") {
        val saksnummer = try {
            call.støtterKunAktiv()
            call.saksnummer()
        } catch (cause: IllegalArgumentException) {
            call.respondJson(
                json = """{"melding":"Ugylidg request ${cause.message}"}""",
                status = HttpStatusCode.BadRequest
            )
            return@get
        }

        val overføringer = overføringRepository.hentAktiveOverføringer(
            saksnummer = setOf(saksnummer)
        )

        if (overføringer.isEmpty()) {
            call.respondJson(json = """{"fått":[],"gitt":[]}""")
            return@get
        }

        require(overføringer.containsKey(saksnummer)) {
            "Fikk overføringer for ${overføringer.keys} ved oppslag på $saksnummer"
        }

        val saksnummerIdentitetsnummerMapping = saksnummerService.hentSaksnummerIdentitetsnummerMapping(
            saksnummer = overføringer.saksnummer()
        )
        // TODO: Legg til tilgangsstyring

        call.respondJson(
            json = overføringer.getValue(saksnummer).somJson(saksnummerIdentitetsnummerMapping)
        )
    }
}


private suspend fun ApplicationCall.respondJson(
    json: String,
    status: HttpStatusCode = HttpStatusCode.OK) = respondText(
    status = status,
    contentType = ContentType.Application.Json,
    text = json
)

private fun GjeldendeOverføringer.somJson(mapping: Map<Saksnummer, Identitetsnummer>) = JSONObject().also { root ->
    root.put("fått", fått.fåttSomJson(mapping))
    root.put("gitt", gitt.gittSomJson(mapping))
}.toString()

private fun List<GjeldendeOverføringGitt>.gittSomJson(mapping: Map<Saksnummer, Identitetsnummer>) = JSONArray().also { array ->
    forEach { gitt -> array.put(gitt.gittSomJson(mapping.getValue(gitt.til))) }
}

private fun List<GjeldendeOverføringFått>.fåttSomJson(mapping: Map<Saksnummer, Identitetsnummer>) = JSONArray().also { array ->
    forEach { fått -> array.put(fått.fåttSomJson(mapping.getValue(fått.fra))) }
}

private fun GjeldendeOverføringFått.fåttSomJson(identitetsnummer: Identitetsnummer) = somJson().also {
    it.put("fra", JSONObject("""{"saksnummer":"$fra", "identitetsnummer": "$identitetsnummer"}"""))
}
private fun GjeldendeOverføringGitt.gittSomJson(identitetsnummer: Identitetsnummer) = somJson().also {
    it.put("til", JSONObject("""{"saksnummer":"$til", "identitetsnummer": "$identitetsnummer"}"""))
    it.put("dagerØnsketOverført", antallDagerØnsketOverført)
    it.put("begrunnelserForPeriode", lovanvendelser!!.somBegrunnelserJson())
}
private fun GjeldendeOverføring.somJson() = JSONObject().also { root ->
    root.put("dagerOverført", antallDager)
    root.put("gjennomført", gjennomført.iso8601())
    root.put("gjelderFraOgMed", "${periode.fom}")
    root.put("gjelderTilOgMed", "${periode.tom}")
    root.put("status", "Aktiv")
}

private fun Lovanvendelser.somBegrunnelserJson() = JSONArray().also { root ->
    kunAnvendelser().forEach { (periode, begrunnelser) ->
        root.put(JSONObject().also {
            it.put("gjelderFraOgMed", "${periode.fom}")
            it.put("gjelderTilOgMed", "${periode.tom}")
            it.put("begrunnelser", begrunnelser)
        })
    }
}

private data class HentOverføringerRequest(
    val saksnummer: Saksnummer,
    val status: Set<String>
)

private val saksnummerRegex  = "[a-zA-Z0-9]{2,10}".toRegex()
private fun ApplicationCall.saksnummer() : Saksnummer {
    return requireNotNull(request.queryParameters["saksnummer"]) {
        "Mangler saksnummer i requesten"
    }.also { saksnummer ->
        require(saksnummer.matches(saksnummerRegex)) {
            "Ugyldig saksnummer $saksnummer"
        }
    }
}
private fun ApplicationCall.støtterKunAktiv() {
    val statuser = requireNotNull(request.queryParameters.getAll("status")) {
        "Mangler parameter status"
    }
    require(statuser.size == 1) { "Støtter kun en status"}
    require(statuser.first() == "Aktiv") { "Støtter ikke status ${statuser.first()}"}
}