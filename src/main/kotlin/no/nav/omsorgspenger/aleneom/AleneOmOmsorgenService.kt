package no.nav.omsorgspenger.aleneom

import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import java.time.LocalDate

internal class AleneOmOmsorgenService(
    private val infotrygdRammeService: InfotrygdRammeService) {

    internal fun hentSpleisetAleneOmOmsorgen(
            identitetsnummer: Identitetsnummer,
            periode: Periode,
            correlationId: CorrelationId): List<SpleisetAleneOmOmsorgen> {
         val aleneOmOmsorgenFraInfotrygd = infotrygdRammeService.hentAleneOmOmsorgen(
                identitetsnummer = identitetsnummer,
                periode = periode,
                correlationId = correlationId
        )

        return aleneOmOmsorgenFraInfotrygd.map {
            SpleisetAleneOmOmsorgen(
                gjennomført = it.vedtatt,
                periode = it.periode,
                barn = Barn(
                    id = it.barn.id,
                    type = it.barn.type,
                    fødselsdato = it.barn.fødselsdato
                ),
                kilder = it.kilder
            )
        }
    }
}

internal data class SpleisetAleneOmOmsorgen(
    val gjennomført: LocalDate,
    val periode: Periode,
    val barn: Barn,
    val kilder: Set<Kilde>
)

internal data class Barn(
    val id: String,
    val type: String,
    val fødselsdato: LocalDate
)