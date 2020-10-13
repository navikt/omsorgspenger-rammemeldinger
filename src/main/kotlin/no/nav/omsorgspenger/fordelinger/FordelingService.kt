package no.nav.omsorgspenger.fordelinger

import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService

internal class FordelingService(
    private val infotrygdRammeService: InfotrygdRammeService) {
    internal fun hentFordelingGirMeldinger(identitetsnummer: Identitetsnummer, periode: Periode) : List<FordelingGirMelding> =
        infotrygdRammeService.hentFordelingGir(
            identitetsnummer = identitetsnummer,
            periode = periode
        ).map { FordelingGirMelding(
            periode = it.periode,
            lengde = it.lengde,
            kilder = it.kilder
        )}
}