package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.sisteDagIÅret
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class OpphøreOverføringerTest(
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
    fun `Opphøre overføringer`() {
        val mor = IdentitetsnummerGenerator.identitetsnummer()
        val saksnummerMor = "foo"
        val far = IdentitetsnummerGenerator.identitetsnummer()
        val saksnummerFar = "bar"
        val barn = overføreOmsorgsdagerBarn(aleneOmOmsorgen = true)

        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            mottaksdato = iDag,
            overføringFra = mor,
            overføringTil = far,
            omsorgsdagerTattUtIÅr = 0,
            omsorgsdagerÅOverføre = 10,
            barn = listOf(barn)
        )

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = mor,
            til = far,
            barn = setOf(barn.identitetsnummer),
            borsammen = true
        )
        rapid.reset()

        hent(mor).also {
            assertThat(it.gitt).hasSize(1)
            assertThat(it.fått).isEmpty()
        }

        hent(far).also {
            assertThat(it.gitt).isEmpty()
            assertThat(it.fått).hasSize(1)
        }

        rapid.opphørOverføringer(
            fra = saksnummerFar,
            til = saksnummerMor,
            fraOgMed = iDag
        )
        rapid.reset()

        // Ingen overføringer å opphøre den vegen, fortsatt samme
        hent(mor).also {
            assertThat(it.gitt).hasSize(1)
            assertThat(it.fått).isEmpty()
        }

        hent(far).also {
            assertThat(it.gitt).isEmpty()
            assertThat(it.fått).hasSize(1)
        }

        rapid.opphørOverføringer(
            fra = saksnummerMor,
            til = saksnummerFar,
            fraOgMed = iDag
        )

        // Alle overføringer opphørt.
        hent(mor).also {
            assertThat(it.gitt).isEmpty()
            assertThat(it.fått).isEmpty()
        }

        hent(far).also {
            assertThat(it.gitt).isEmpty()
            assertThat(it.fått).isEmpty()
        }
    }

    private fun hent(identitetsnummer: Identitetsnummer) = applicationContext.spleisetOverføringerService.hentSpleisetOverføringer(
        identitetsnummer = identitetsnummer,
        periode = periode,
        correlationId = correlationId
    )

    private companion object {
        private val iDag = LocalDate.now()
        private val periode = Periode(fom = iDag, tom = iDag.sisteDagIÅret())
        private val correlationId = "${UUID.randomUUID()}"
    }
}