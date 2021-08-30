package no.nav.omsorgspenger.aleneom

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class AleneOmOmsorgenRepositoryTest(
    dataSource: DataSource) {

    private val migratedDataSource = dataSource.cleanAndMigrate()

    private val aleneOmOmsorgenRepository = AleneOmOmsorgenRepository(
        dataSource = migratedDataSource
    )

    private val aleneOmOmsorgenTestRepository = AleneOmOmsorgenTestRepository(
        dataSource = migratedDataSource
    )

    @Test
    fun `lagre og hente alene om omsorgen`() {
        val saksnummer1 = "Sak-1"
        val saksnummer2 = "Sak-2"

        // Initielt ingen
        assertThat(saksnummer1.hent()).isEmpty()
        assertThat(saksnummer2.hent()).isEmpty()

        val saksnummer1AleneOmOmsorgenFor = setOf(AleneOmOmsorgenFor(
            identitetsnummer = "Id-1",
            fødselsdato = LocalDate.parse("2018-05-13"),
            aleneOmOmsorgenI = Periode("2020-01-01/2020-05-05")
        ))

        aleneOmOmsorgenTestRepository.lagre(
            saksnummer = saksnummer1,
            behovssekvensId = "Behovssekvens-1",
            registreresIForbindelseMed = AleneOmOmsorgenTestRepository.RegistreresIForbindelseMed.Overføring,
            aleneOmOmsorgenFor = saksnummer1AleneOmOmsorgenFor
        )

        val forventetAleneOmOmsorgenSaksnummer1 = setOf(
            AleneOmOmsorgen(
                registrert = registrert,
                periode = Periode("2020-01-01/2020-05-05"),
                barn = AleneOmOmsorgen.Barn(
                    identitetsnummer = "Id-1",
                    fødselsdato = LocalDate.parse("2018-05-13")
                ),
                behovssekvensId = "Behovssekvens-1",
                regstrertIForbindelseMed = "Overføring"
            )
        )

        // Lagrer på saksnummer 1
        assertThat(saksnummer1.hent()).hasSameElementsAs(forventetAleneOmOmsorgenSaksnummer1)
        assertThat(saksnummer2.hent()).isEmpty()

        aleneOmOmsorgenTestRepository.lagre(
            saksnummer = saksnummer1,
            behovssekvensId = "Behovssekvens-2",
            registreresIForbindelseMed = AleneOmOmsorgenTestRepository.RegistreresIForbindelseMed.Overføring,
            aleneOmOmsorgenFor = saksnummer1AleneOmOmsorgenFor
        )

        // Lagrer samme barn på ny skal ikke ha noen effekt
        assertThat(saksnummer1.hent()).hasSameElementsAs(forventetAleneOmOmsorgenSaksnummer1)
        assertThat(saksnummer2.hent()).isEmpty()

        val saksnummer2AleneOmOmsorgenFor = setOf(AleneOmOmsorgenFor(
            identitetsnummer = "Id-2",
            fødselsdato = LocalDate.parse("2017-05-13"),
            aleneOmOmsorgenI = Periode("2020-02-01/2020-08-05")
        ), AleneOmOmsorgenFor(
            identitetsnummer = "Id-3",
            fødselsdato = LocalDate.parse("2020-05-13"),
            aleneOmOmsorgenI = Periode("2021-02-01/2022-08-05")
        ))

        aleneOmOmsorgenTestRepository.lagre(
            saksnummer = saksnummer2,
            behovssekvensId = "Behovssekvens-3",
            registreresIForbindelseMed = AleneOmOmsorgenTestRepository.RegistreresIForbindelseMed.Overføring,
            aleneOmOmsorgenFor = saksnummer2AleneOmOmsorgenFor
        )

        // Lagrer 2 barn på saksnummer 2
        val forventetAleneOmOmsorgenSaksnummer2 = setOf(AleneOmOmsorgen(
                registrert = registrert,
                periode = Periode("2020-02-01/2020-08-05"),
                barn = AleneOmOmsorgen.Barn(
                    identitetsnummer = "Id-2",
                    fødselsdato = LocalDate.parse("2017-05-13")
                ),
                behovssekvensId = "Behovssekvens-3",
                regstrertIForbindelseMed = "Overføring"
        ), AleneOmOmsorgen(
            registrert = registrert,
            periode = Periode("2021-02-01/2022-08-05"),
            barn = AleneOmOmsorgen.Barn(
                identitetsnummer = "Id-3",
                fødselsdato = LocalDate.parse("2020-05-13")
            ),
            behovssekvensId = "Behovssekvens-3",
            regstrertIForbindelseMed = "Overføring"
        ))
        assertThat(saksnummer1.hent()).hasSameElementsAs(forventetAleneOmOmsorgenSaksnummer1)
        assertThat(saksnummer2.hent()).hasSameElementsAs(forventetAleneOmOmsorgenSaksnummer2)
    }

    private fun Saksnummer.hent() = aleneOmOmsorgenRepository.hent(
        saksnummer = this
    ).map { it.copy(registrert = registrert) }

    private companion object {
        private val registrert = ZonedDateTime.now()
    }
}