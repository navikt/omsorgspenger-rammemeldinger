package no.nav.omsorgspenger.testutils

import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.statistikk.StatistikkMelding
import no.nav.omsorgspenger.statistikk.StatistikkService

internal class RecordingStatistikkService : StatistikkService {
    private val statistikkMeldinger = mutableListOf<StatistikkMelding>()

    override fun publiser(statistikkMelding: StatistikkMelding) {
        statistikkMeldinger.add(statistikkMelding)
    }

    internal fun finnStatistikkMeldingFor(behovssekvensId: BehovssekvensId) = statistikkMeldinger
        .filter { it.behandlingId == behovssekvensId }
        .also { require(it.size == 1) }
        .first()
}