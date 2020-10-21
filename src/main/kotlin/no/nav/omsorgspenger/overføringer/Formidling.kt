package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.BehovssekvensId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.formidling.Melding
import no.nav.omsorgspenger.formidling.Meldingsbestilling
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerBehandlingMelding
import no.nav.omsorgspenger.overføringer.meldinger.OverføreOmsorgsdagerMelding
import org.intellij.lang.annotations.Language
import java.time.LocalDate

internal object Fordmidling {
    internal fun opprettMeldingsBestillinger(
        behovssekvensId: BehovssekvensId,
        personopplysninger: Map<Identitetsnummer, Personopplysninger>,
        overføreOmsorgsdager: OverføreOmsorgsdagerMelding.Behovet,
        behandling: OverføreOmsorgsdagerBehandlingMelding.ForVidereBehandling) : List<Meldingsbestilling> {

        val saksnummer = behandling
            .gjeldendeOverføringer
            .entries
            .map { it.key to it.value.saksnummer }
            .toMap()

        require(personopplysninger.size == saksnummer.size && personopplysninger.keys.containsAll(saksnummer.keys)) {
            "Mismatch mellom saksnummer og personopplysninger."
        }

        val bestillinger = mutableListOf<Meldingsbestilling>()

        saksnummer.forEach { (identitetsnummer, saksnummer) ->

            val melding = when (identitetsnummer) {
                overføreOmsorgsdager.overførerFra -> GittDager(
                    til = personopplysninger.getValue(overføreOmsorgsdager.overførerTil),
                    overføringer = behandling.overføringer
                )
                overføreOmsorgsdager.overførerTil -> MottattDager(
                    fra = personopplysninger.getValue(overføreOmsorgsdager.overførerFra),
                    overføringer = behandling.overføringer
                )
                else -> TidligerePartner(
                    fraOgMed = overføreOmsorgsdager.mottaksdato.plusDays(1)
                )
            }

            bestillinger.add(Meldingsbestilling(
                behovssekvensId = behovssekvensId,
                aktørId = personopplysninger.getValue(identitetsnummer).aktørId,
                saksnummer = saksnummer,
                melding = melding,
                // Alle parter får svaret i brev om den som overfører dager
                // sendte inn per brev...
                måSendesSomBrev = behandling.måBesvaresPerBrev()
            ))
        }
        return bestillinger
    }
}

internal class GittDager(
    val til: Personopplysninger,
    val overføringer: List<Overføring>
) : Melding {
    override val mal = "OVERFORE_GITT_DAGER"
    override val data = {
        @Language("JSON")
        val json = """
            {
            }
        """.trimIndent()
        json
    }()
}

internal class MottattDager(
    val fra: Personopplysninger,
    val overføringer: List<Overføring>
) : Melding {
    override val mal = "OVERFORE_MOTTATT_DAGER"
    override val data = {
        @Language("JSON")
        val json = """
            {
            }
        """.trimIndent()
        json
    }()
}

internal class TidligerePartner(
    val fraOgMed: LocalDate
) : Melding {
    override val mal = "OVERFORE_TIDLIGERE_PARTNER"
    override val data = {
        @Language("JSON")
        val json = """
            {
              "fraOgMed": "$fraOgMed"
            }
        """.trimIndent()
        json
    }()
}