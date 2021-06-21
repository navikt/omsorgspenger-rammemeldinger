package no.nav.omsorgspenger.midlertidigalene

import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService

internal class MidlertidigAleneService(
    private val infotrygdRammeService: InfotrygdRammeService) {
    internal suspend fun hentMidlertidigAleneVedtak(
        identitetsnummer: Identitetsnummer,
        periode: Periode,
        correlationId: CorrelationId) =
        infotrygdRammeService.hentMidlertidigAlene(
            identitetsnummer = identitetsnummer,
            periode = periode,
            correlationId = correlationId
        ).map { MidlertidigAleneVedtak(
            periode = it.periode,
            kilder = it.kilder
        )}
}