package no.nav.omsorgspenger.testutils

import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.formidling.FormidlingService
import no.nav.omsorgspenger.formidling.Meldingsbestilling

internal class RecordingFormidlingService : FormidlingService {

    private val meldingsbestillinger = mutableListOf<Meldingsbestilling>()

    override fun sendMeldingsbestillinger(meldingsbestillinger: List<Meldingsbestilling>) {
        this.meldingsbestillinger.addAll(meldingsbestillinger)
    }

    internal fun finnMeldingsbestillingerFor(behovssekvensId: BehovssekvensId) = meldingsbestillinger.filter {
        it.behovssekvensId == behovssekvensId
    }
}