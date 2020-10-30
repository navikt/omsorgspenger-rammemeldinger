package no.nav.omsorgspenger.aleneom

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.LocalDate

fun Route.AleneOmApi() {
    post("/hentAleneOmOmsorgen") {
        val request = call.receive<RammemeldingerRequest>()
        // todo: hent faktiske data

        call.respond(status = HttpStatusCode.OK, message = AleneOmOmsorgenResponseDto(aleneOmOmsorgen = listOf()))
    }
}

private data class RammemeldingerRequest(val identitetsnummer: String, val fom: LocalDate, val tom: LocalDate)

private data class PersonDto(val id: String, val type: String, val fødselsdato: LocalDate)

private data class AleneOmOmsorgenResponseDto(val aleneOmOmsorgen: List<AleneOmOmsorgenDto>)
private data class AleneOmOmsorgenDto(
        val gjennomført: LocalDate,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val barn: PersonDto
)