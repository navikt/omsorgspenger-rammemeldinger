package no.nav.omsorgspenger.koronaoverføringer.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.koronaoverføringer.rivers.KoronaoverføringerRapidVerktøy.gjennomførKoronaOverføring
import no.nav.omsorgspenger.koronaoverføringer.rivers.KoronaoverføringerRapidVerktøy.opphørKoronaoverføringer
import no.nav.omsorgspenger.overføringer.apis.Motpart
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringFått
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringGitt
import no.nav.omsorgspenger.overføringer.apis.SpleisetOverføringer
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class OpphøreKoronaOverføringerTest(
    dataSource: DataSource) {
    private val applicationContext = TestApplicationContextBuilder(
        dataSource = dataSource.cleanAndMigrate()
    ).build()

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContext)
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Opphøre koronaoverføringer`() {
        val mor = IdentitetsnummerGenerator.identitetsnummer()
        val far = IdentitetsnummerGenerator.identitetsnummer()
        val morSaksnummer = "OP$mor"
        val farSaksnummer = "OP$far"
        val barn = koronaBarn()

        rapid.gjennomførKoronaOverføring(
            fra = mor,
            til = far,
            fraSaksnummer = morSaksnummer,
            tilSaksnummer = farSaksnummer,
            barn = listOf(barn),
            omsorgsdagerÅOverføre = 20,
            omsorgsdagerTattUtIÅr = 7,
            mottatt = Mottatt
        )

        rapid.reset()
        rapid.gjennomførKoronaOverføring(
            fra = far,
            til = mor,
            fraSaksnummer = farSaksnummer,
            tilSaksnummer = morSaksnummer,
            barn = listOf(barn),
            omsorgsdagerÅOverføre = 33,
            omsorgsdagerTattUtIÅr = 0,
            mottatt = Mottatt
        )

        rapid.reset()
        rapid.gjennomførKoronaOverføring(
            fra = far,
            til = mor,
            fraSaksnummer = farSaksnummer,
            tilSaksnummer = morSaksnummer,
            barn = listOf(barn),
            omsorgsdagerÅOverføre = 13,
            omsorgsdagerTattUtIÅr = 0,
            mottatt = Mottatt
        )
        rapid.reset()

        hent(mor).also {
            assertThat(it.gitt).hasSameElementsAs(setOf(
                forventetGitt(til = far, periode = UtÅret, antallDager = 13),
            ))
            assertThat(it.fått).hasSameElementsAs(setOf(
                forventetFått(fra = far, periode = UtÅret, antallDager = 20)
            ))
        }

        hent(far).also {
            assertThat(it.gitt).hasSameElementsAs(setOf(
                forventetGitt(til = mor, periode = UtÅret, antallDager = 20),
            ))
            assertThat(it.fått).hasSameElementsAs(setOf(
                forventetFått(fra = mor, periode = UtÅret, antallDager = 13),
            ))
        }


        rapid.opphørKoronaoverføringer(
            fra = morSaksnummer,
            til = farSaksnummer,
            fraOgMed = IDag
        )
        rapid.reset()

        hent(mor).also {
            assertThat(it.gitt).isEmpty()
            assertThat(it.fått).hasSameElementsAs(setOf(
                forventetFått(fra = far, periode = UtÅret, antallDager = 20)
            ))
        }

        hent(far).also {
            assertThat(it.gitt).hasSameElementsAs(setOf(
                forventetGitt(til = mor, periode = UtÅret, antallDager = 20),
            ))
            assertThat(it.fått).isEmpty()
        }

        rapid.opphørKoronaoverføringer(
            fra = farSaksnummer,
            til = morSaksnummer,
            fraOgMed = OmEnUke
        )
        rapid.reset()

        val forventetPeriode = Periode(fom = IDag, tom = OmEnUke.minusDays(1))
        hent(mor).also {
            assertThat(it.gitt).isEmpty()
            assertThat(it.fått).hasSameElementsAs(setOf(
                forventetFått(fra = far, periode = forventetPeriode, antallDager = 20)
            ))
        }

        hent(far).also {
            assertThat(it.gitt).hasSameElementsAs(setOf(
                forventetGitt(til = mor, periode = forventetPeriode, antallDager = 20),
            ))
            assertThat(it.fått).isEmpty()
        }

    }

    private fun hent(identitetsnummer: Identitetsnummer) = runBlocking { applicationContext.spleisetKoronaOverføringerService.hentSpleisetOverføringer(
        identitetsnummer = identitetsnummer,
        periode = UtÅret,
        correlationId = "${UUID.randomUUID()}"
    ).sammenlignbar()}

    private companion object {
        private val IDag = LocalDate.now().withYear(2021)
        private val Mottatt =  ZonedDateTime.now().withYear(2021).minusHours(5)
        private val OmEnUke = IDag.plusDays(1)
        private val UtÅret = Periode(fom = IDag, tom = LocalDate.parse("2022-12-31"))

        private fun SpleisetOverføringer.sammenlignbar() = SpleisetOverføringer(
            gitt = gitt.map { it.copy(kilder = emptySet(), gjennomført = IDag) },
            fått = fått.map { it.copy(kilder = emptySet(), gjennomført = IDag) }
        )

        private fun forventetGitt(til: Identitetsnummer, periode: Periode, antallDager: Int) = SpleisetOverføringGitt(
            gjennomført = IDag,
            gyldigFraOgMed = periode.fom,
            gyldigTilOgMed = periode.tom,
            til = Motpart(type = "Identitetsnummer", id = til),
            lengde = Duration.ofDays(antallDager.toLong()),
            kilder = emptySet()
        )

        private fun forventetFått(fra: Identitetsnummer, periode: Periode, antallDager: Int) = SpleisetOverføringFått(
            gjennomført = IDag,
            gyldigFraOgMed = periode.fom,
            gyldigTilOgMed = periode.tom,
            fra = Motpart(type = "Identitetsnummer", id = fra),
            lengde = Duration.ofDays(antallDager.toLong()),
            kilder = emptySet()
        )
    }
}