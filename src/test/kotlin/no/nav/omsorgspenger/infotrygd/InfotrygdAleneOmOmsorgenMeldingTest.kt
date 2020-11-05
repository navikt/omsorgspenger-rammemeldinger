package no.nav.omsorgspenger.infotrygd

import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class InfotrygdAleneOmOmsorgenMeldingTest {

    @Test
    internal fun barn() {
        assertThat(melding(id = "12345678900", barnetsFødselsdato = LocalDate.parse("2020-01-01")).barn)
                .isEqualTo(InfotrygdAnnenPart(
                        id = "12345678900",
                        fødselsdato = LocalDate.parse("2020-01-01"),
                        type = "Identitetsnummer"
                ))

        assertThat(melding(id = null, barnetsFødselsdato = LocalDate.parse("2020-01-01")).barn)
                .isEqualTo(InfotrygdAnnenPart(
                        id = "010120",
                        fødselsdato = LocalDate.parse("2020-01-01"),
                        type = "Fødselsdato"
                ))
    }

    private fun melding(id: Identitetsnummer?, barnetsFødselsdato: LocalDate): InfotrygdAleneOmOmsorgenMelding {
        return InfotrygdAleneOmOmsorgenMelding(
                periode = Periode("2020-01-01/2020-01-01"),
                vedtatt = LocalDate.parse("2020-01-01"),
                kilder = setOf(),
                barnetsFødselsdato = barnetsFødselsdato,
                barnetsIdentitetsnummer = id
        )
    }
}