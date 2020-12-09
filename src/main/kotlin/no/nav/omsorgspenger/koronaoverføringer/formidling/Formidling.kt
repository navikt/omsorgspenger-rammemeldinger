package no.nav.omsorgspenger.koronaoverføringer.formidling

import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.formidling.Meldingsbestilling
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerMelding
import no.nav.omsorgspenger.koronaoverføringer.meldinger.OverføreKoronaOmsorgsdagerPersonopplysningerMelding
import no.nav.omsorgspenger.overføringer.Personopplysninger
import org.slf4j.LoggerFactory

internal object Formidling {
    private val logger = LoggerFactory.getLogger(Formidling::class.java)
    private fun meldingMåSendesManuelt(karakteristikk: String) =
        logger.warn("Melding(er) må sendes manuelt. Støtter ikke melding(er) for $karakteristikk. Se sikker logg for informasjon til melding(ene)").let { false }

    internal fun opprettMeldingsbestillinger(
        behovssekvensId: BehovssekvensId,
        personopplysninger: Map<Identitetsnummer, OverføreKoronaOmsorgsdagerPersonopplysningerMelding.Personopplysninger>,
        behovet: OverføreKoronaOmsorgsdagerMelding.Behovet,
        behandling: OverføreKoronaOmsorgsdagerBehandlingMelding.ForVidereBehandling
        ) : List<Meldingsbestilling> {

        val adressebeskyttet = personopplysninger.filterKeys {
            it == behovet.fra || it == behovet.til
        }.also { require(it.size == 2) }.any { it.value.adressebeskyttet }

        if (adressebeskyttet) {
            meldingMåSendesManuelt("adresssebeskyttede parter")
            return emptyList()
        }

        val meldingsbestillinger = mutableListOf<Meldingsbestilling>()

        if (behandling.overføring.antallDager <= 0) {
            // Avslagsmelding til den som forsøkte å overføre dager
            meldingsbestillinger.add(Meldingsbestilling(
                behovssekvensId = behovssekvensId,
                aktørId = personopplysninger.getValue(behovet.fra).aktørId,
                saksnummer = behandling.fraSaksnummer,
                måBesvaresPerBrev = false,
                melding = Avslag(
                    mottaksdato = behovet.mottaksdato,
                    antallDagerØnsketOverført = behovet.omsorgsdagerÅOverføre,
                    til = personopplysninger.getValue(behovet.til)
                )
            ))
            require(meldingsbestillinger.size == 1)
        } else {
            // Melding til den som overfører
            meldingsbestillinger.add(Meldingsbestilling(
                behovssekvensId = behovssekvensId,
                aktørId = personopplysninger.getValue(behovet.fra).aktørId,
                saksnummer = behandling.fraSaksnummer,
                måBesvaresPerBrev = false,
                melding = GittDager(
                    mottaksdato = behovet.mottaksdato,
                    antallDagerØnsketOverført = behovet.omsorgsdagerÅOverføre,
                    til = personopplysninger.getValue(behovet.til),
                    overføring = behandling.overføring
                )
            ))
            // Melding til den som mottar
            meldingsbestillinger.add(Meldingsbestilling(
                behovssekvensId = behovssekvensId,
                aktørId = personopplysninger.getValue(behovet.til).aktørId,
                saksnummer = behandling.tilSaksnummer,
                måBesvaresPerBrev = false,
                melding = MottattDager(
                    mottaksdato = behovet.mottaksdato,
                    fra = personopplysninger.getValue(behovet.fra),
                    overføring = behandling.overføring
                )
            ))
            require(meldingsbestillinger.size == 2)
        }

        return meldingsbestillinger
    }
}