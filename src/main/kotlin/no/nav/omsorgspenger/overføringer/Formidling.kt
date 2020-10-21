package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.BehovssekvensId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.formidling.Melding
import no.nav.omsorgspenger.formidling.Meldingsbestilling
import java.time.LocalDate

internal object Fordmidling {
    internal fun opprettMeldingsBestillinger(
        behovssekvensId: BehovssekvensId,
        personopplysninger: Map<Identitetsnummer, Personopplysninger>,
        saksnummer: Map<Identitetsnummer, Saksnummer>,
        overføringFra: Identitetsnummer,
        overføringTil: Identitetsnummer,
        måSendesSomBrev: Boolean,
        mottaksdato: LocalDate,
        overføringer: List<Overføring>) : List<Meldingsbestilling> {

        require(personopplysninger.size == saksnummer.size && personopplysninger.keys.containsAll(saksnummer.keys)) {
            "Mismatch mellom saksnummer og personopplysninger."
        }

        val bestillinger = mutableListOf<Meldingsbestilling>()

        saksnummer.forEach { (identitetsnummer, saksnummer) ->

            val melding = when (identitetsnummer) {
                overføringFra -> GittDager(
                    til = personopplysninger.getValue(overføringTil),
                    overføringer = overføringer
                )
                overføringTil -> MottattDager(
                    fra = personopplysninger.getValue(overføringFra),
                    overføringer = overføringer
                )
                else -> TidligerePartner(
                    fom = mottaksdato.plusDays(1)
                )
            }

            bestillinger.add(Meldingsbestilling(
                behovssekvensId = behovssekvensId,
                aktørId = personopplysninger.getValue(identitetsnummer).aktørId,
                saksnummer = saksnummer,
                melding = melding,
                // Alle parter får svaret i brev om den som overfører dager
                // sendte inn per brev...
                måSendesSomBrev = måSendesSomBrev
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
    override val data: String
        get() = """
            {
            }
        """.trimIndent()
}

internal class MottattDager(
    val fra: Personopplysninger,
    val overføringer: List<Overføring>
) : Melding {
    override val mal = "OVERFORE_MOTTATT_DAGER"
    override val data: String
        get() = """
            {
            }
        """.trimIndent()
}

internal class TidligerePartner(
    val fom: LocalDate
) : Melding {
    override val mal = "OVERFORE_TIDLIGERE_PARTNER"
    override val data: String
        get() = """
            {
            }
        """.trimIndent()
}