package no.nav.omsorgspenger.personopplysninger

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

internal class VurderRelasjonerMeldingTest {

    @Test
    fun `behovskontrakt`() {
        val behov = VurderRelasjonerMelding.BehovInput(
            identitetsnummer = "1000",
            til = setOf("1001", "1002")
        )
        val actual = VurderRelasjonerMelding.behov(behov).json.toString()

        @Language("json")
        val expected = """
                    {
                          "identitetsnummer":"1000",
                          "til":[
                             "1001",
                             "1002"
                          ]
                    }
        """.trimIndent()

        JSONAssert.assertEquals(expected, actual, true)
    }
}