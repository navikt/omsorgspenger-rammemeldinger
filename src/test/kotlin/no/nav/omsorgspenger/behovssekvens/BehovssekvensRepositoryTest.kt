package no.nav.omsorgspenger.behovssekvens

import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import javax.sql.DataSource
import kotlin.test.assertEquals


@ExtendWith(DataSourceExtension::class)
internal class BehovssekvensRepositoryTest(
    dataSource: DataSource){
    private val behovssekvensRepository = BehovssekvensRepository(
        dataSource = dataSource.cleanAndMigrate()
    )

    @Test
    fun `Test lagring og henting av behovssekvenser`() {

        @Language("JSON")
        val behovssekvensJson = """
            {
              "test": true,
              "behovssekvens": {
                "foo": "bar"
              }
            }
        """.trimIndent()


        behovssekvensRepository.harHåndtert(
            behovssekvensId = "1",
            behovssekvens = behovssekvensJson,
            steg = "En"
        )

        assertFalse(behovssekvensRepository.skalHåndtere(
            behovssekvensId = "1",
            steg = "En"
        ))
        assertTrue(behovssekvensRepository.skalHåndtere(
            behovssekvensId = "1",
            steg = "To"
        ))
        assertTrue(behovssekvensRepository.skalHåndtere(
            behovssekvensId = "2",
            steg = "En"
        ))

        val behovssekvenser = behovssekvensRepository.hent(
            behovssekvensId = "1"
        )

        assertEquals(1, behovssekvenser.size)
        assertEquals("1", behovssekvenser.first().behovssekvensId)
        assertEquals("En", behovssekvenser.first().gjennomførtSteg)
        JSONAssert.assertEquals(behovssekvensJson, behovssekvenser.first().behovssekvens, true)
    }

}