package no.nav.omsorgspenger.overføringer

import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.overføringer.gjennomføring.OverføringRepository
import no.nav.omsorgspenger.testutils.DataSourceExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class OverføringRepositoryTest(
    dataSource: DataSource){

    private val overføringRepository = OverføringRepository(
        dataSource = dataSource
    )

    @Test
    fun `Gjennomfør overføringer`() {
        val res = overføringRepository.gjennomførOverføringer(
            fra = "sak1",
            til = "sak2",
            overføringer = listOf(
                Overføring(
                    antallDager = 1,
                    periode = Periode("2020-01-01/2021-01-01"),
                    starterGrunnet = listOf(),
                    slutterGrunnet = listOf()
                )
            )
        )

        println(res)

        val res2 = overføringRepository.gjennomførOverføringer(
            fra = "sak1",
            til = "sak2",
            overføringer = listOf(
                Overføring(
                    antallDager = 5,
                    periode = Periode("2020-05-01/2021-01-01"),
                    starterGrunnet = listOf(),
                    slutterGrunnet = listOf()
                )
            )
        )

        println(res2)
    }
}