package no.nav.omsorgspenger.aleneom

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.InfotrygdAleneOmOmsorgenMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdAnnenPart
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class AleneOmOmsorgenServiceTest {

    @MockK
    lateinit var infotrygdRammeService: InfotrygdRammeService

    lateinit var aleneOmOmsorgenService: AleneOmOmsorgenService

    @BeforeEach
    internal fun setUp() {
        aleneOmOmsorgenService = AleneOmOmsorgenService(infotrygdRammeService = infotrygdRammeService)
    }

    @Test
    internal fun name() {
        val identitetsnummer = "01019012345"
        val correlationId = "uuid"
        val søkeperiode = Periode("2020-01-01/2020-12-30")

        val periode = Periode("2020-02-02/2020-02-03")
        val vedtatt = LocalDate.parse("2020-02-02")
        val barnetsFødselsdato = LocalDate.parse("2019-01-01")
        val barnetsIdentitetsnummer = "01011912345"
        every {
            infotrygdRammeService.hentAleneOmOmsorgen(identitetsnummer, søkeperiode, correlationId)
        }.returns(listOf(InfotrygdAleneOmOmsorgenMelding(
                periode = periode,
                vedtatt = vedtatt,
                kilder = setOf(),
                barnetsFødselsdato = barnetsFødselsdato,
                barnetsIdentitetsnummer = barnetsIdentitetsnummer
        )))

        val resultat = aleneOmOmsorgenService.hentAleneOmOmsorgen(identitetsnummer, søkeperiode, correlationId)

        assertThat(resultat).containsExactly(AleneOmOmsorgen(
                gjennomført = vedtatt,
                periode = periode,
                barn = InfotrygdAnnenPart(id = barnetsIdentitetsnummer, fødselsdato = barnetsFødselsdato, type = "Identitetsnummer"),
                kilder = setOf()
        ))
    }
}