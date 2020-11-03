package no.nav.omsorgspenger.aleneom

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgspenger.Periode
import java.time.LocalDate

internal fun Route.AleneOmApi(aleneOmOmsorgenService: AleneOmOmsorgenService) {
    post("/hentAleneOmOmsorgen") {
        val request = call.receive<RammemeldingerRequest>()
        // todo: hent faktiske data
        val aleneOmOmosrgen = aleneOmOmsorgenService.hentAleneOmOmsorgen(request.identitetsnummer, Periode(request.fom, request.tom), "correlation-id") // TODO

        val result = AleneOmOmsorgenResponseDto(
                aleneOmOmsorgen = aleneOmOmosrgen.map {
                    AleneOmOmsorgenDto(
                            gjennomført = TODO(),
                            gyldigFraOgMed = it.periode.fom,
                            gyldigTilOgMed = it.periode.tom,
                            barn = PersonDto(
                                    id = it.annenPart.id,
                                    type = it.annenPart.type,
                                    fødselsdato = it.annenPart.fødselsdato
                            )
                    )
                }
        )

        call.respond(status = HttpStatusCode.OK, message = result)
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