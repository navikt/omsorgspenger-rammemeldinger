package no.nav.omsorgspenger.koronaoverføringer.apis

import KoronaoverføringRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.infotrygd.*
import no.nav.omsorgspenger.overføringer.*
import no.nav.omsorgspenger.overføringer.apis.Motpart
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringFått
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

internal class SpleisetKoronaOverføringerServiceTest {

    private val infotrygdRammeServiceMock = mockk<InfotrygdRammeService>()

    private val saksnummerServiceMock = mockk<SaksnummerService>()

    private val koronaoverføringRepositoryMock = mockk<KoronaoverføringRepository>()

    private val spleisetOverføringerService = SpleisetKoronaOverføringerService(
        infotrygdRammeService = infotrygdRammeServiceMock,
        saksnummerService = saksnummerServiceMock,
        koronaoverføringRepository = koronaoverføringRepositoryMock
    )

    @BeforeEach
    fun reset() {
        clearMocks(infotrygdRammeServiceMock, saksnummerServiceMock, koronaoverføringRepositoryMock)
    }

    @Test
    fun `Ingen overføringer`() {
        mockInfotrygd()
        mockNyLøsning()
        mockSaksnummer()
        val spleisetOverføringer = hentSpleisetOverføringer(periode = I2021)
        verify(exactly = 1) { saksnummerServiceMock.hentSaksnummer(any()) }
        verify(exactly = 1) { koronaoverføringRepositoryMock.hentAlleOverføringer(any()) }
        verify(exactly = 1) { infotrygdRammeServiceMock.hentKoronaOverføringFår(any(), any(), any()) }
        verify(exactly = 1) { infotrygdRammeServiceMock.hentKoronaOverføringGir(any(), any(), any()) }
        assertThat(spleisetOverføringer.gitt).isEmpty()
        assertThat(spleisetOverføringer.fått).isEmpty()
    }

    @Test
    fun `Både i ny løsning og infotrygd`() {
        val gjennomført = ZonedDateTime.parse("2021-01-10T12:15:00.000+01:00")
        val vedtatt = LocalDate.parse("2021-01-10")
        val periode = I2021

        mockInfotrygd(
            fått = listOf(InfotrygdKoronaOverføringFårMelding(
                periode = periode,
                vedtatt = vedtatt,
                kilder = setOf(Kilde(type = "foo", id = "bar")),
                lengde = Duration.ofDays(2),
                fra = InfotrygdAnnenPart(id = "foo", type ="bar", fødselsdato = vedtatt.minusDays(10))
            )),
            gitt = listOf(InfotrygdKoronaOverføringGirMelding(
                periode = periode,
                vedtatt = vedtatt,
                kilder = setOf(Kilde(type = "foo2", id = "bar2")),
                lengde = Duration.ofDays(5),
                til = InfotrygdAnnenPart(id = "foo2", type ="bar2", fødselsdato = vedtatt.minusDays(5))
            ))
        )

        mockNyLøsning(gjeldendeOverføringer = mapOf(
            saksnummer to GjeldendeOverføringer(
                fått = listOf(GjeldendeOverføringFått(
                    gjennomført = gjennomført,
                    antallDager = 5,
                    periode = periode,
                    status = GjeldendeOverføring.Status.Aktiv,
                    fra = "678",
                    kilder = kilderNyLøsning
                )),
                gitt = listOf(GjeldendeOverføringGitt(
                    gjennomført = gjennomført,
                    antallDager = 3,
                    periode = periode,
                    status = GjeldendeOverføring.Status.Aktiv,
                    til = "456",
                    kilder = kilderNyLøsning,
                    antallDagerØnsketOverført = 5
                ))
            )
        ))

        val spleisetOverføringer = hentSpleisetOverføringer(periode = periode)
        assertThat(spleisetOverføringer.fått).hasSameElementsAs(setOf(
            SpleisetOverføringFått(
                gjennomført = vedtatt,
                gyldigFraOgMed = periode.fom,
                gyldigTilOgMed = periode.tom,
                fra = Motpart(id = "foo", type = "bar"),
                lengde = Duration.ofDays(2),
                kilder = setOf(Kilde(type = "foo", id = "bar"))
            ),
            SpleisetOverføringFått(
                gjennomført = vedtatt,
                gyldigFraOgMed = periode.fom,
                gyldigTilOgMed = periode.tom,
                fra = Motpart(id = "ID-678", type = "Identitetsnummer"),
                lengde = Duration.ofDays(5),
                kilder = kilderNyLøsning
            )
        ))
        assertThat(spleisetOverføringer.gitt).hasSameElementsAs(setOf(
            SpleisetOverføringGitt(
                gjennomført = vedtatt,
                gyldigFraOgMed = periode.fom,
                gyldigTilOgMed = periode.tom,
                til = Motpart(id = "foo2", type = "bar2"),
                lengde = Duration.ofDays(5),
                kilder = setOf(Kilde(type = "foo2", id = "bar2"))
            ),
            SpleisetOverføringGitt(
                gjennomført = vedtatt,
                gyldigFraOgMed = periode.fom,
                gyldigTilOgMed = periode.tom,
                til = Motpart(id = "ID-456", type = "Identitetsnummer"),
                lengde = Duration.ofDays(3),
                kilder = kilderNyLøsning
            )
        ))
    }

    @Test
    fun `Før 2021`() {
        val vedtatt = LocalDate.now()
        mockInfotrygd(fått = listOf(InfotrygdKoronaOverføringFårMelding(
            periode = I2020,
            vedtatt = vedtatt,
            kilder = setOf(Kilde(type = "foo", id = "bar")),
            lengde = Duration.ofDays(2),
            fra = InfotrygdAnnenPart(id = "foo", type ="bar", fødselsdato = vedtatt.minusDays(10)),
        )), gitt = listOf())

        val spleisetOverføringer = hentSpleisetOverføringer(
            periode = I2020
        )

        verify(exactly = 0) { saksnummerServiceMock.hentSaksnummer(any()) }
        verify(exactly = 0) { koronaoverføringRepositoryMock.hentAlleOverføringer(any()) }
        verify(exactly = 1) { infotrygdRammeServiceMock.hentKoronaOverføringFår(any(), any(), any()) }
        verify(exactly = 1) { infotrygdRammeServiceMock.hentKoronaOverføringGir(any(), any(), any()) }

        assertThat(spleisetOverføringer.gitt).isEmpty()
        assertThat(spleisetOverføringer.fått).hasSameElementsAs(setOf(
            SpleisetOverføringFått(
                gjennomført = vedtatt,
                gyldigFraOgMed = I2020.fom,
                gyldigTilOgMed = I2020.tom,
                fra = Motpart(id = "foo", type = "bar"),
                lengde = Duration.ofDays(2),
                kilder = setOf(Kilde(type = "foo", id = "bar"))
            )
        ))
    }

    @Test
    fun `Etter 2021`() {
        val vedtatt = LocalDate.now()
        mockInfotrygd(fått = listOf(InfotrygdKoronaOverføringFårMelding(
            periode = Etter2021,
            vedtatt = vedtatt,
            kilder = setOf(Kilde(type = "foo", id = "bar")),
            lengde = Duration.ofDays(2),
            fra = InfotrygdAnnenPart(id = "foo", type ="bar", fødselsdato = vedtatt.minusDays(10)),
        )), gitt = listOf())

        val spleisetOverføringer = hentSpleisetOverføringer(
            periode = Etter2021
        )

        verify(exactly = 0) { saksnummerServiceMock.hentSaksnummer(any()) }
        verify(exactly = 0) { koronaoverføringRepositoryMock.hentAlleOverføringer(any()) }
        verify(exactly = 1) { infotrygdRammeServiceMock.hentKoronaOverføringFår(any(), any(), any()) }
        verify(exactly = 1) { infotrygdRammeServiceMock.hentKoronaOverføringGir(any(), any(), any()) }

        assertThat(spleisetOverføringer.gitt).isEmpty()
        assertThat(spleisetOverføringer.fått).hasSameElementsAs(setOf(
            SpleisetOverføringFått(
                gjennomført = vedtatt,
                gyldigFraOgMed = Etter2021.fom,
                gyldigTilOgMed = Etter2021.tom,
                fra = Motpart(id = "foo", type = "bar"),
                lengde = Duration.ofDays(2),
                kilder = setOf(Kilde(type = "foo", id = "bar"))
            )
        ))
    }

    private fun mockInfotrygd(
        gitt: List<InfotrygdKoronaOverføringGirMelding> = listOf(),
        fått: List<InfotrygdKoronaOverføringFårMelding> = listOf()) {
        every { infotrygdRammeServiceMock.hentKoronaOverføringGir(any(), any(), any()) }.returns(gitt)
        every { infotrygdRammeServiceMock.hentKoronaOverføringFår(any(), any(), any()) }.returns(fått)
    }

    private fun mockNyLøsning(
        gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer> = mapOf()) {
        val saksnummer = gjeldendeOverføringer.saksnummer()
        every { koronaoverføringRepositoryMock.hentAlleOverføringer(any()) }.returns(gjeldendeOverføringer)
        every { saksnummerServiceMock.hentSaksnummer(identitetsnummer) }.returns(gjeldendeOverføringer.keys.firstOrNull())
        val saksnummerIdentitetsnummerMapping = mutableMapOf<Saksnummer, Identitetsnummer>().also {
            saksnummer.forEach { sak -> it[sak] = "ID-$sak" }
        }
        every { saksnummerServiceMock.hentSaksnummerIdentitetsnummerMapping(saksnummer) }.returns(saksnummerIdentitetsnummerMapping)
    }

    private fun mockSaksnummer() {
        every { saksnummerServiceMock.hentSaksnummer(identitetsnummer) }.returns(saksnummer)
    }

    private fun hentSpleisetOverføringer(
        periode: Periode
    ) = spleisetOverføringerService.hentSpleisetOverføringer(
        identitetsnummer = identitetsnummer,
        periode = periode,
        correlationId = correlationId
    )

    private companion object {
        private val kilderNyLøsning = setOf(
            Kilde.internKilde("1", "KoronaOverføring")
        )
        private const val saksnummer = "123"
        private const val identitetsnummer = "11111111111"

        private val I2020 = Periode("2020-01-01/2020-12-31")
        private val I2021 = Periode("2021-01-05/2021-06-15")
        private val Etter2021 = Periode("2022-01-01/2022-02-10")

        private val correlationId = "test"
    }
}