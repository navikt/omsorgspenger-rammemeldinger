package no.nav.omsorgspenger.overføringer

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.Duration
import java.time.LocalDate

fun Route.OverføringerApi() {
    post("/hentOverfoeringer") {
        val request = call.receive<RammemeldingerRequest>()
        // todo: hent faktiske data

        call.respond(status = HttpStatusCode.OK, message = OverføringResponseDto(gitt = listOf(), fått = listOf()))
    }

    post("/hentAleneOmOmsorgen") {
        val request = call.receive<RammemeldingerRequest>()
        // todo: hent faktiske data

        call.respond(status = HttpStatusCode.OK, message = AleneOmOmsorgenResponseDto(aleneOmOmsorgen = listOf()))
    }
}


private data class RammemeldingerRequest(val personIdent: String, val fom: LocalDate, val tom: LocalDate)

private data class OverføringResponseDto(val gitt: List<OverføringGittDto>, val fått: List<OverføringFåttDto>)
private data class OverføringGittDto(
        val gjennomført: LocalDate,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val til: PersonDto,
        val lengde: Duration
)
private data class OverføringFåttDto(
        val gjennomført: LocalDate,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val fra: PersonDto,
        val lengde: Duration
)

private data class PersonDto(val id: String, val type: String, val fødselsdato: LocalDate)

private data class AleneOmOmsorgenResponseDto(val aleneOmOmsorgen: List<AleneOmOmsorgenDto>)
private data class AleneOmOmsorgenDto(
        val gjennomført: LocalDate,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val barn: PersonDto
)