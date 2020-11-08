package no.nav.omsorgspenger.aleneom

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgspenger.Periode
import java.time.LocalDate
import java.util.*

internal fun Route.AleneOmApi(aleneOmOmsorgenService: AleneOmOmsorgenService) {
    post("/hentAleneOmOmsorgen") {
        val request = call.receive<RammemeldingerRequest>()
        val correlationId = call.request.header(HttpHeaders.XCorrelationId) ?: UUID.randomUUID().toString()

        val aleneOmOmsorgen = aleneOmOmsorgenService.hentAleneOmOmsorgen(
            identitetsnummer = request.identitetsnummer,
            periode = Periode(request.fom, request.tom),
            correlationId = correlationId
        )

        val result = AleneOmOmsorgenResponseDto(
                aleneOmOmsorgen = aleneOmOmsorgen.map {
                    AleneOmOmsorgenDto(
                            gjennomført = it.gjennomført,
                            gyldigFraOgMed = it.periode.fom,
                            gyldigTilOgMed = it.periode.tom,
                            barn = AnnenPartDto(
                                    id = it.barn.id,
                                    type = it.barn.type,
                                    fødselsdato = it.barn.fødselsdato
                            )
                    )
                }
        )

        call.respond(status = HttpStatusCode.OK, message = result)
    }
}

private data class RammemeldingerRequest(val identitetsnummer: String, val fom: LocalDate, val tom: LocalDate)

private data class AnnenPartDto(val id: String, val type: String, val fødselsdato: LocalDate)

private data class AleneOmOmsorgenResponseDto(val aleneOmOmsorgen: List<AleneOmOmsorgenDto>)
private data class AleneOmOmsorgenDto(
        val gjennomført: LocalDate,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val barn: AnnenPartDto
)