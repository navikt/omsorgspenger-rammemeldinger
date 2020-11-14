package no.nav.omsorgspenger.aleneom

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.infotrygd.InfotrygdAleneOmOmsorgenMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdAnnenPart
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

internal class AleneOmOmsorgenServiceTest {
    private val infotrygdRammeServiceMock = mockk<InfotrygdRammeService>()

    private val saksnummerServiceMock = mockk<SaksnummerService>()

    private val aleneOmOmsorgenRepository = mockk<AleneOmOmsorgenRepository>()

    private val aleneOmOmsorgenService = AleneOmOmsorgenService(
        infotrygdRammeService = infotrygdRammeServiceMock,
        saksnummerService = saksnummerServiceMock,
        aleneOmOmsorgenRepository = aleneOmOmsorgenRepository
    )

    @BeforeEach
    fun reset() {
        clearMocks(infotrygdRammeServiceMock, saksnummerServiceMock, aleneOmOmsorgenRepository)
    }

    @Test
    fun `kun registrert i infotrygd`() {
        mockInfotrygd(listOf(InfotrygdAleneOmOmsorgenMelding(
            periode = Periode("2020-01-01/2021-01-01"),
            vedtatt = LocalDate.parse("2018-01-01"),
            kilder = setOf(Kilde(id= "123", type = "Personkort")),
            barn = InfotrygdAnnenPart(id = "2019-01-01", type = "Fødselsdato", fødselsdato = LocalDate.parse("2019-01-01"))
        )))
        mockNyLøsning(emptySet())

        val forventet = listOf(SpleisetAleneOmOmsorgen(
            registrert = LocalDate.parse("2018-01-01"),
            gyldigFraOgMed = LocalDate.parse("2020-01-01"),
            gyldigTilOgMed = LocalDate.parse("2021-01-01"),
            barn = SpleisetAleneOmOmsorgen.Barn(
                id = "2019-01-01",
                type = "Fødselsdato",
                fødselsdato = LocalDate.parse("2019-01-01")
            ),
            kilder = setOf(Kilde(id= "123", type = "Personkort"))
        ))

        assertThat(hentSpleiset()).hasSameElementsAs(forventet)
    }

    @Test
    fun `kun registrert i ny løsning`() {
        val registrert = ZonedDateTime.parse("2020-11-10T12:15:00.000+01:00")
        mockInfotrygd(emptyList())
        mockNyLøsning(setOf(AleneOmOmsorgen(
            registrert = registrert,
            periode = Periode("2020-01-01/2025-02-02"),
            barn = AleneOmOmsorgen.Barn(
                identitetsnummer = "1234",
                fødselsdato = LocalDate.parse("2017-02-06")
            ),
            behovssekvensId = "Behovssekvens-1",
            regstrertIForbindelseMed = "Overføring"
        )))

        val forventet = listOf(SpleisetAleneOmOmsorgen(
            registrert = LocalDate.parse("2020-11-10"),
            gyldigFraOgMed = LocalDate.parse("2020-01-01"),
            gyldigTilOgMed = LocalDate.parse("2025-02-02"),
            barn = SpleisetAleneOmOmsorgen.Barn(
                id = "1234",
                type = "Identitetsnummer",
                fødselsdato = LocalDate.parse("2017-02-06")
            ),
            kilder = setOf(Kilde(id= "Behovssekvens-1", type = "OmsorgspengerRammemeldinger[Overføring]"))
        ))

        assertThat(hentSpleiset()).hasSameElementsAs(forventet)
    }

    @Test
    fun `registret både i infotrygd og ny løsning`() {
        val registrert = ZonedDateTime.parse("2020-11-10T12:15:00.000+01:00")
        // To i ny løsning
        mockNyLøsning(setOf(AleneOmOmsorgen(
            registrert = registrert,
            periode = Periode("2020-01-01/2025-02-02"),
            barn = AleneOmOmsorgen.Barn(
                identitetsnummer = "1234",
                fødselsdato = LocalDate.parse("2017-02-06")
            ),
            behovssekvensId = "Behovssekvens-1",
            regstrertIForbindelseMed = "Overføring"
        ), AleneOmOmsorgen(
            registrert = registrert,
            periode = Periode("2020-01-06/2025-02-27"),
            barn = AleneOmOmsorgen.Barn(
                identitetsnummer = "5678",
                fødselsdato = LocalDate.parse("2017-02-12")
            ),
            behovssekvensId = "Behovssekvens-1",
            regstrertIForbindelseMed = "Overføring"
        )))
        // Tre i infotrygd, hvorav 2 er de samme som i ny løsning
        mockInfotrygd(listOf(InfotrygdAleneOmOmsorgenMelding(
            periode = Periode("2020-01-02/2021-01-02"),
            vedtatt = LocalDate.parse("2018-01-02"),
            kilder = setOf(Kilde(id= "123", type = "Personkort")),
            // Unikt barn i infotrygd
            barn = InfotrygdAnnenPart(id = "2019-01-05", type = "Fødselsdato", fødselsdato = LocalDate.parse("2019-01-05"))
        ), InfotrygdAleneOmOmsorgenMelding(
            periode = Periode("2020-01-03/2021-01-03"),
            vedtatt = LocalDate.parse("2018-01-03"),
            kilder = setOf(Kilde(id= "456", type = "Personkort")),
            // Samme fødselsdato som i ny løsning
            barn = InfotrygdAnnenPart(id = "2017-02-12", type = "Fødselsdato", fødselsdato = LocalDate.parse("2017-02-12"))
        ), InfotrygdAleneOmOmsorgenMelding(
            periode = Periode("2020-01-04/2021-01-04"),
            vedtatt = LocalDate.parse("2018-01-03"),
            kilder = setOf(Kilde(id= "789", type = "Personkort")),
            // Samme identitetsnummer som i ny løsning
            barn = InfotrygdAnnenPart(id = "1234", type = "Identitetsnummer", fødselsdato = LocalDate.parse("1999-01-01"))
        )))

        val forventet = listOf(SpleisetAleneOmOmsorgen(
            registrert = LocalDate.parse("2020-11-10"),
            gyldigFraOgMed = LocalDate.parse("2020-01-01"),
            gyldigTilOgMed = LocalDate.parse("2025-02-02"),
            barn = SpleisetAleneOmOmsorgen.Barn(id = "1234", type = "Identitetsnummer", fødselsdato = LocalDate.parse("2017-02-06")),
            kilder = setOf(Kilde(id= "Behovssekvens-1", type = "OmsorgspengerRammemeldinger[Overføring]"))
        ), SpleisetAleneOmOmsorgen(
            registrert = LocalDate.parse("2020-11-10"),
            gyldigFraOgMed = LocalDate.parse("2020-01-06"),
            gyldigTilOgMed = LocalDate.parse("2025-02-27"),
            barn = SpleisetAleneOmOmsorgen.Barn(id = "5678", type = "Identitetsnummer", fødselsdato = LocalDate.parse("2017-02-12")),
            kilder = setOf(Kilde(id= "Behovssekvens-1", type = "OmsorgspengerRammemeldinger[Overføring]"))
        ), SpleisetAleneOmOmsorgen(
            registrert = LocalDate.parse("2018-01-02"),
            gyldigFraOgMed = LocalDate.parse("2020-01-02"),
            gyldigTilOgMed = LocalDate.parse("2021-01-02"),
            barn = SpleisetAleneOmOmsorgen.Barn(id = "2019-01-05", type = "Fødselsdato", fødselsdato = LocalDate.parse("2019-01-05")),
            kilder = setOf(Kilde(id= "123", type = "Personkort"))
        ))

        assertThat(hentSpleiset()).hasSameElementsAs(forventet)
    }


    private fun mockInfotrygd(
        aleneOmOmsorgen: List<InfotrygdAleneOmOmsorgenMelding> = listOf()) {
        every { infotrygdRammeServiceMock.hentAleneOmOmsorgen(any(), any(), any()) }.returns(aleneOmOmsorgen)
    }

    private fun mockNyLøsning(
        aleneOmOmsorgen: Set<AleneOmOmsorgen>) {
        every { saksnummerServiceMock.hentSaksnummer(any()) }.returns(Saksnummer)
        every { aleneOmOmsorgenRepository.hent(Saksnummer) }.returns(aleneOmOmsorgen)
    }

    private fun hentSpleiset() = aleneOmOmsorgenService.hentSpleisetAleneOmOmsorgen(
        identitetsnummer = Identitsnummer,
        periode = periode,
        correlationId = CorrelationId
    )

    private companion object {
        private const val Identitsnummer = "Id-1"
        private const val Saksnummer = "123"
        private val periode = Periode("2020-01-01/2020-12-31")
        private const val CorrelationId = "test"
    }
}