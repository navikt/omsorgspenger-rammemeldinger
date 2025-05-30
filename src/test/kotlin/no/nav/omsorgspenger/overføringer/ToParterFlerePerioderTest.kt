package no.nav.omsorgspenger.overføringer

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.extensions.sisteDagIÅret
import no.nav.omsorgspenger.fordelinger.FordelingGirMelding
import no.nav.omsorgspenger.fordelinger.FordelingService
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.IdentitetsnummerGenerator.identitetsnummer
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import no.nav.omsorgspenger.utvidetrett.UtvidetRettService
import no.nav.omsorgspenger.utvidetrett.UtvidetRettVedtak
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class ToParterFlerePerioderTest(
    dataSource: DataSource){
    private val fordelingService = mockk<FordelingService>()
    private val utvidetRettService = mockk<UtvidetRettService>()

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(TestApplicationContextBuilder(
            dataSource = dataSource.cleanAndMigrate()
        ).also { builder ->
            builder.fordelingService = fordelingService
            builder.utvidetRettService = utvidetRettService
        }.build())
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Brukt dager i år i kombinasjon med fordeling`() {
        val fra = identitetsnummer()
        val til = identitetsnummer()
        val mottaksdato = LocalDate.parse("2020-09-29")
        val fødselsdatoBarnUtvidetRett= mottaksdato.minusYears(17)
        val fødsesldatoYngsteBarn = mottaksdato.minusYears(5)

        coEvery { fordelingService.hentFordelingGirMeldinger(any(), any(), any()) }
            .returns(listOf(FordelingGirMelding(
                periode = Periode(
                    fom = fødsesldatoYngsteBarn.plusMonths(6),
                    tom = fødsesldatoYngsteBarn.plusYears(12).sisteDagIÅret()
                ),
                lengde = Duration.ofDays(15),
                kilder = setOf()
            )))


        coEvery { utvidetRettService.hentUtvidetRettVedtak(any(), any(), any()) }
            .returns(listOf(UtvidetRettVedtak(
                periode = Periode(
                    fom = fødselsdatoBarnUtvidetRett.plusWeeks(3),
                    tom = fødselsdatoBarnUtvidetRett.plusDays(18).sisteDagIÅret()
                ),
                barnetsFødselsdato = fødselsdatoBarnUtvidetRett,
                kilder = setOf()
            )))

        val barn = listOf(
            OverføreOmsorgsdagerBehov.Barn(
                identitetsnummer = identitetsnummer(),
                fødselsdato = fødselsdatoBarnUtvidetRett,
                utvidetRett = true,
                aleneOmOmsorgen = false
            ),
            OverføreOmsorgsdagerBehov.Barn(
                identitetsnummer = identitetsnummer(),
                fødselsdato = mottaksdato.minusYears(11),
                utvidetRett = false,
                aleneOmOmsorgen = false
            ),
            OverføreOmsorgsdagerBehov.Barn(
                identitetsnummer = identitetsnummer(),
                fødselsdato = mottaksdato.minusYears(5),
                aleneOmOmsorgen = true,
                utvidetRett = false
            )
        )


        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            overføringFra = fra,
            overføringTil = til,
            omsorgsdagerTattUtIÅr = 17,
            omsorgsdagerÅOverføre = 10,
            mottaksdato = mottaksdato,
            barn = barn
        )

        rapid.ventPåLøsning(
            behovssekvens = behovssekvens,
            fra = fra,
            til = til,
            barn = barn.map { it.identitetsnummer }.toSet(),
            borsammen = true
        )

        val (_, løsning) = rapid.løsningOverføreOmsorgsdager()

        assertTrue(løsning.erGjennomført())
        løsning.overføringer.assertOverføringer(
            fra = fra,
            til = til,
            forventedeOverføringer = mapOf(
                Periode("2020-09-29/2020-12-31") to 3, // 35 omsorgsdager - 15 grunnrett - 15 fordelt - 2 tatt ut utover grunnrett = 3
                Periode("2021-01-01/2021-12-31") to 5, // 35 omsorgdager - 15 grunnrett - 15 fordelt = 5
            )
        )
    }
}