package no.nav.omsorgspenger.aleneom.apis

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.k9.rapid.behov.Behovsformat.iso8601
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.aleneom.AleneOmOmsorgenRepository
import org.json.JSONObject

internal fun Route.AleneOmOmsorgenApi(
    aleneOmOmsorgenRepository: AleneOmOmsorgenRepository) {

    get("/alene-om-omsorgen") {
        val saksnummer : Saksnummer = call.request.queryParameters["saksnummer"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        val dtos = aleneOmOmsorgenRepository.hent(saksnummer).map { aleneOmOmsorgen -> JSONObject().also { json ->
            json.put("registrert", aleneOmOmsorgen.registrert.iso8601())
            json.put("gjelderFraOgMed", "${aleneOmOmsorgen.periode.fom}")
            json.put("gjelderTilOgMed", "${aleneOmOmsorgen.periode.tom}")
            json.put("barn", JSONObject().also { barnJson ->
                barnJson.put("identitetsnummer", aleneOmOmsorgen.barn.identitetsnummer)
                barnJson.put("fødselsdato", "${aleneOmOmsorgen.barn.fødselsdato}")
            })
        }}

        call.respondText(
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json,
            text = JSONObject().also { root -> root.put("aleneOmOmsorgen", dtos) }.toString()
        )
    }
}