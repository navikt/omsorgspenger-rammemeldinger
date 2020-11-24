package no.nav.omsorgspenger.overføringer.apis

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.infotrygd.InfotrygdAnnenPart
import no.nav.omsorgspenger.infotrygd.InfotrygdOverføringFårMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdOverføringGirMelding
import no.nav.omsorgspenger.infotrygd.InfotrygdRammeService
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringer
import no.nav.omsorgspenger.overføringer.db.OverføringRepository
import no.nav.omsorgspenger.overføringer.saksnummer
import no.nav.omsorgspenger.saksnummer.SaksnummerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

internal class SpleisetOverføringerServiceTest {

    private val infotrygdRammeServiceMock = mockk<InfotrygdRammeService>()

    private val saksnummerServiceMock = mockk<SaksnummerService>()

    private val overføringRepositoryMock = mockk<OverføringRepository>()

    private val spleisetOverføringerService = SpleisetOverføringerService(
        infotrygdRammeService = infotrygdRammeServiceMock,
        saksnummerService = saksnummerServiceMock,
        overføringRepository = overføringRepositoryMock
    )

    @BeforeEach
    fun reset() {
        clearMocks(infotrygdRammeServiceMock, saksnummerServiceMock, overføringRepositoryMock)
    }

    @Test
    fun `Ingen overføringer`() {
        mockInfotrygd()
        mockNyLøsning()
        val spleisetOverføringer = hentSpleisetOverføringer()
        assertThat(spleisetOverføringer.gitt).isEmpty()
        assertThat(spleisetOverføringer.fått).isEmpty()
    }

    @Test
    fun `Kun overføringer i Infotrygd`() {
        val vedtatt = LocalDate.now()
        mockInfotrygd(fått = listOf(InfotrygdOverføringFårMelding(
            periode = periode,
            vedtatt = vedtatt,
            kilder = setOf(Kilde(type = "foo", id = "bar")),
            lengde = Duration.ofDays(2),
            fra = InfotrygdAnnenPart(id = "foo", type ="bar", fødselsdato = vedtatt.minusDays(10)),
        )), gitt = listOf())
        mockNyLøsning(gjeldendeOverføringer = mapOf())

        val spleisetOverføringer = hentSpleisetOverføringer()
        assertThat(spleisetOverføringer.fått).hasSameElementsAs(listOf(SpleisetOverføringFått(
            gjennomført = vedtatt,
            kilder = setOf(Kilde(type = "foo", id = "bar")),
            gyldigFraOgMed = periode.fom,
            gyldigTilOgMed = periode.tom,
            fra = Motpart(id= "foo", type= "bar"),
            lengde = Duration.ofDays(2)
        )))

        assertThat(spleisetOverføringer.gitt).isEmpty()
    }

    @Test
    fun `Kun overføringer i ny løsning`() {
        val gjennomført = ZonedDateTime.parse("2020-11-10T12:15:00.000+01:00")

        mockInfotrygd()
        mockNyLøsning(gjeldendeOverføringer = mapOf(
            saksnummer to GjeldendeOverføringer(
                gitt = listOf(GjeldendeOverføringGitt(
                    gjennomført = gjennomført,
                    antallDager = 5,
                    periode = periode,
                    status = GjeldendeOverføring.Status.Aktiv,
                    til = "456",
                    kilder = kilderNyLøsning
                ))
            )
        ))

        val spleisetOverføringer = hentSpleisetOverføringer()
        assertThat(spleisetOverføringer.gitt).hasSameElementsAs(listOf(SpleisetOverføringGitt(
            gjennomført = LocalDate.parse("2020-11-10"),
            kilder = kilderNyLøsning,
            gyldigFraOgMed = periode.fom,
            gyldigTilOgMed = periode.tom,
            til = Motpart(id = "ID-456", type= "Identitetsnummer"),
            lengde = Duration.ofDays(5)
        )))

        assertThat(spleisetOverføringer.fått).isEmpty()

    }

    @Test
    fun `Nyere overføringer i ny løsning enn i Infotrygd`() {
        val gjennomført = ZonedDateTime.parse("2020-11-10T12:15:00.000+01:00")
        val vedtatt = LocalDate.parse("2020-11-09")

        mockInfotrygd(fått = listOf(InfotrygdOverføringFårMelding(
            periode = periode,
            vedtatt = vedtatt,
            kilder = setOf(Kilde(type = "foo", id = "bar")),
            lengde = Duration.ofDays(2),
            fra = InfotrygdAnnenPart(id = "foo", type ="bar", fødselsdato = vedtatt.minusDays(10)),
        )), gitt = listOf())

        mockNyLøsning(gjeldendeOverføringer = mapOf(
            saksnummer to GjeldendeOverføringer(
                gitt = listOf(GjeldendeOverføringGitt(
                    gjennomført = gjennomført,
                    antallDager = 5,
                    periode = periode,
                    status = GjeldendeOverføring.Status.Aktiv,
                    til = "456",
                    kilder = kilderNyLøsning
                ))
            )
        ))

        val spleisetOverføringer = hentSpleisetOverføringer()
        assertThat(spleisetOverføringer.gitt).hasSameElementsAs(listOf(SpleisetOverføringGitt(
            gjennomført = LocalDate.parse("2020-11-10"),
            kilder = kilderNyLøsning,
            gyldigFraOgMed = periode.fom,
            gyldigTilOgMed = periode.tom,
            til = Motpart(id = "ID-456", type= "Identitetsnummer"),
            lengde = Duration.ofDays(5)
        )))

        assertThat(spleisetOverføringer.fått).isEmpty()
    }

    @Test
    fun `Nyere overføringer i Infotrygd enn i ny løsning`() {
        val gjennomført = ZonedDateTime.parse("2020-11-10T12:15:00.000+01:00")
        val vedtatt = LocalDate.parse("2020-11-12")

        mockInfotrygd(fått = listOf(InfotrygdOverføringFårMelding(
            periode = periode,
            vedtatt = vedtatt,
            kilder = setOf(Kilde(type = "foo", id = "bar")),
            lengde = Duration.ofDays(2),
            fra = InfotrygdAnnenPart(id = "foo", type ="bar", fødselsdato = vedtatt.minusDays(10)),
        )), gitt = listOf())

        mockNyLøsning(gjeldendeOverføringer = mapOf(
            saksnummer to GjeldendeOverføringer(
                gitt = listOf(GjeldendeOverføringGitt(
                    gjennomført = gjennomført,
                    antallDager = 5,
                    periode = periode,
                    status = GjeldendeOverføring.Status.Aktiv,
                    til = "456",
                    kilder = setOf()
                ))
            )
        ))

        val spleisetOverføringer = hentSpleisetOverføringer()

        assertThat(spleisetOverføringer.fått).hasSameElementsAs(listOf(SpleisetOverføringFått(
            gjennomført = vedtatt,
            kilder = setOf(Kilde(type = "foo", id = "bar")),
            gyldigFraOgMed = periode.fom,
            gyldigTilOgMed = periode.tom,
            fra = Motpart(id= "foo", type= "bar"),
            lengde = Duration.ofDays(2)
        )))

        assertThat(spleisetOverføringer.gitt).isEmpty()
    }

    private fun mockInfotrygd(
        gitt: List<InfotrygdOverføringGirMelding> = listOf(),
        fått: List<InfotrygdOverføringFårMelding> = listOf()) {
        every { infotrygdRammeServiceMock.hentOverføringGir(any(), any(), any()) }.returns(gitt)
        every { infotrygdRammeServiceMock.hentOverføringFår(any(), any(), any()) }.returns(fått)
    }

    private fun mockNyLøsning(
        gjeldendeOverføringer: Map<Saksnummer, GjeldendeOverføringer> = mapOf()) {
        val saksnummer = gjeldendeOverføringer.saksnummer()
        every { overføringRepositoryMock.hentAktiveOverføringer(any()) }.returns(gjeldendeOverføringer)
        every { saksnummerServiceMock.hentSaksnummer(identitetsnummer) }.returns(gjeldendeOverføringer.keys.firstOrNull())
        val saksnummerIdentitetsnummerMapping = mutableMapOf<Saksnummer, Identitetsnummer>().also {
            saksnummer.forEach { sak -> it[sak] = "ID-$sak" }
        }
        every { saksnummerServiceMock.hentSaksnummerIdentitetsnummerMapping(saksnummer) }.returns(saksnummerIdentitetsnummerMapping)
    }

    private fun hentSpleisetOverføringer() = spleisetOverføringerService.hentSpleisetOverføringer(
        identitetsnummer = identitetsnummer,
        periode = periode,
        correlationId = correlationId
    )

    private companion object {
        private val kilderNyLøsning = setOf(
            Kilde.internKilde("1", "Overføring"),
            Kilde.internKilde("2", "Overføring")
        )
        private const val saksnummer = "123"
        private const val identitetsnummer = "11111111111"
        private val periode = Periode("2020-01-01/2020-12-31")
        private val correlationId = "test"
    }
}