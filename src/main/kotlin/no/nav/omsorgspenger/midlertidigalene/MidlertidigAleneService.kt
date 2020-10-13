package no.nav.omsorgspenger.midlertidigalene

import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService

internal class MidlertidigAleneService(
    private val infotrygdRammeService: InfotrygdRammeService) {
    internal fun hentMidlertidigAleneVedtak(identitetsnummer: Identitetsnummer, periode: Periode) =
        infotrygdRammeService.hentMidlertidigAlene(
            identitetsnummer = identitetsnummer,
            periode = periode
        ).map { MidlertidigAleneVedtak(
            periode = it.periode,
            kilder = it.kilder
        )}
}