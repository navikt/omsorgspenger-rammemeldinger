package no.nav.omsorgspenger.utvidetrett

import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService

internal class UtvidetRettService(
    private val infotrygdRammeService: InfotrygdRammeService) {
    internal fun hentUtvidetRettVedtak(
        identitetsnummer: Identitetsnummer,
        periode: Periode,
        correlationId: CorrelationId) : List<UtvidetRettVedtak> =
        infotrygdRammeService.hentUtvidetRett(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        ).map { UtvidetRettVedtak(
            periode = it.periode,
            barnetsFødselsdato = it.barnetsFødselsdato,
            barnetsIdentitetsnummer = it.barnetsIdentitetsnummer,
            kilder = it.kilder
        )}
}