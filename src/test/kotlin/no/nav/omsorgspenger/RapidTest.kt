package no.nav.omsorgspenger

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RapidTest {

    val rapid = TestRapid().apply { OmsorgspengerRammemeldinger(this) }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `dette ær ett test`() {
        rapid.sendTestMessage("""
             {"@id":"test"}
        """.trimIndent())

        assertEquals(1, rapid.inspektør.size)
    }


}