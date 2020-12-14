package no.nav.omsorgspenger.overføringer

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.TestApplicationContextBuilder
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.test.assertEquals

@ExtendWith(DataSourceExtension::class)
internal class OverføringerPåVentTest(
    dataSource: DataSource) {
    private val rapid = TestRapid().apply {
        this.registerApplicationContext(
            TestApplicationContextBuilder(dataSource.cleanAndMigrate()).build()
        )
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `Overføring mottatt i 2021 behandles ikke`() {
        val (_, behovssekvens) = behovssekvensOverføreOmsorgsdager(
            mottaksdato = LocalDate.parse("2021-01-01"),
            barn = listOf(overføreOmsorgsdagerBarn())
        )
        rapid.sendTestMessage(behovssekvens)
        assertEquals(0, rapid.inspektør.size)
    }
}