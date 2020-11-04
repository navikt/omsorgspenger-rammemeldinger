package no.nav.omsorgspenger.aleneom

import no.nav.omsorgspenger.AnnenPart
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService

internal class AleneOmOmsorgenService(
        private val infotrygdRammeService: InfotrygdRammeService) {

    internal fun hentAleneOmOmsorgen(
            identitetsnummer: Identitetsnummer,
            periode: Periode,
            correlationId: CorrelationId): List<AleneOmOmsorgen> {
         val rammemeldinger = infotrygdRammeService.hentAleneOmOmsorgen(
                identitetsnummer = identitetsnummer,
                periode = periode,
                correlationId = correlationId
        )

        return rammemeldinger.map {
            AleneOmOmsorgen(
                    gjennomført = it.vedtatt,
                    periode = it.periode,
                    barn = AnnenPart(it.barnetsIdentitetsnummer ?: TODO(), "Identitetsnummer", it.barnetsFødselsdato),
                    kilder = it.kilder,
            )
        }
    }
}