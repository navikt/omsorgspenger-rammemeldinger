package no.nav.omsorgspenger.overføringer.formidling

import no.nav.omsorgspenger.BehovssekvensId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.formidling.Meldingsbestilling
import no.nav.omsorgspenger.overføringer.Personopplysninger
import no.nav.omsorgspenger.overføringer.formidling.StartOgSluttGrunnUtleder.startOgSluttGrunn
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import org.slf4j.LoggerFactory

internal object Formidling {
    internal fun opprettMeldingsBestillinger(
        behovssekvensId: BehovssekvensId,
        personopplysninger: Map<Identitetsnummer, Personopplysninger>,
        overføreOmsorgsdager: OverføreOmsorgsdagerMelding.Behovet,
        behandling: OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling) : List<Meldingsbestilling> {

        val formidlingsoverføringer = Formidlingsoverføringer(
            overføreOmsorgsdager = overføreOmsorgsdager,
            behandling = behandling,
            personopplysninger = personopplysninger
        )

        if (!formidlingsoverføringer.støtterAutomatiskMelding) {
            return listOf()
        }

        val bestillinger = mutableListOf<Meldingsbestilling>()

        behandling.saksnummer.forEach { (identitetsnummer, saksnummer) ->
            val melding = when (identitetsnummer) {
                overføreOmsorgsdager.overførerFra -> GittDager(
                    til = personopplysninger.getValue(overføreOmsorgsdager.overførerTil),
                    formidlingsoverføringer = formidlingsoverføringer,
                    antallDagerØnsketOverført = overføreOmsorgsdager.omsorgsdagerÅOverføre,
                    mottaksdato = overføreOmsorgsdager.mottaksdato
                )
                overføreOmsorgsdager.overførerTil -> MottattDager.melding(
                    fra = personopplysninger.getValue(overføreOmsorgsdager.overførerFra),
                    formidlingsoverføringer = formidlingsoverføringer,
                    mottaksdato = overføreOmsorgsdager.mottaksdato
                )
                else -> TidligerePartner.melding(
                    mottaksdato = overføreOmsorgsdager.mottaksdato,
                    formidlingsoverføringer = formidlingsoverføringer
                )
            }

            melding?.also {
                bestillinger.add(Meldingsbestilling(
                    behovssekvensId = behovssekvensId,
                    aktørId = personopplysninger.getValue(identitetsnummer).aktørId,
                    saksnummer = saksnummer,
                    melding = melding,
                    // Alle parter får svaret i brev om den som overfører dager
                    // sendte inn per brev...
                    måBesvaresPerBrev = behandling.måBesvaresPerBrev
                ))
            }

        }

        return bestillinger
    }
}


internal class Formidlingsoverføringer(
    overføreOmsorgsdager: OverføreOmsorgsdagerMelding.Behovet,
    behandling: OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling,
    personopplysninger: Map<Identitetsnummer, Personopplysninger>) {

    internal val alleOverføringer =
        behandling.overføringer
    internal val avslåtteOverføringer =
        alleOverføringer.filter { it.antallDager == 0 }
    internal val innvilgedeOverføringer =
        alleOverføringer.filter { it.antallDager == overføreOmsorgsdager.omsorgsdagerÅOverføre }
    internal val delvisInnvilgedeOverføringer =
        alleOverføringer.minus(avslåtteOverføringer).minus(innvilgedeOverføringer)
    internal val utenAvslåtteOverføringer =
        alleOverføringer.filter { it.antallDager != 0 }

    internal val innvilget =
        avslåtteOverføringer.isEmpty() &&
        delvisInnvilgedeOverføringer.isEmpty() &&
        innvilgedeOverføringer.size == 1

    internal val avslått =
        avslåtteOverføringer.size == 1 &&
        delvisInnvilgedeOverføringer.isEmpty() &&
        innvilgedeOverføringer.isEmpty()

    internal val startOgSluttGrunn = startOgSluttGrunn(
        innvilget = innvilget,
        avslått = avslått,
        alleOverføringer = alleOverføringer,
        innvilgedeOverføringer = innvilgedeOverføringer,
        delvisInnvilgedeOverføringer = delvisInnvilgedeOverføringer,
        avslåtteOverføringer = avslåtteOverføringer
    )

    internal val støtterAutomatiskMelding = when {
        personopplysninger.values.any { it.adressebeskyttet } -> meldingMåSendesManuelt("adresssebeskyttede parter")
        behandling.oppfyllerIkkeInngangsvilkår -> meldingMåSendesManuelt("avslag på inngangsvilkår")
        behandling.overføringer.size !in 1..2 -> meldingMåSendesManuelt("${behandling.overføringer.size} overføringer")
        startOgSluttGrunn == null -> meldingMåSendesManuelt("dette scenarioet")
        else -> true
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Formidlingsoverføringer::class.java)
        private fun meldingMåSendesManuelt(karakteristikk: String) =
            logger.warn("Melding(er) må sendes manuelt. Støtter ikke melding(er) for $karakteristikk. Se sikker logg for informasjon til melding(ene)").let { false }
    }
}