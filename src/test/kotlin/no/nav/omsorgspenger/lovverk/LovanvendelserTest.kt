package no.nav.omsorgspenger.lovverk

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.omsorgspenger.Periode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

internal class LovanvendelserTest {

    @Test
    fun `serialisering og deserialiseringstest`() {

        val periode1 = Periode("2019-01-01/2019-03-05")
        val periode2 = Periode("2020-02-03/2020-05-07")
        val periode3 = Periode("2021-12-30/2022-02-02")

        val lovanvendelser = Lovanvendelser()
        lovanvendelser
            .leggTil(
                periode = periode1,
                lovhenvisning = Lovhenvisning1, anvendelse = "En to tre § lov"
            ).leggTil(
                periode = periode2,
                lovhenvisning = Lovhenvisning2, anvendelse = "By design"
            ).leggTil(
                periode = periode3,
                lovhenvisning = Lovhenvisning3, anvendelse = "Det var som bare.."
            ).leggTil(
                periode = periode1,
                lovhenvisning = Lovhenvisning1, anvendelse = "Samme periode."
            )

        @Language("JSON")
        val forventetJson = """
        {
            "2019-01-01/2019-03-05": {
                "Min lov (versjon 1) § 1-2 Noe kult, første ledd, andre punktum": ["En to tre § lov", "Samme periode."]
            },
            "2020-02-03/2020-05-07": {
                "Min lov (versjon 1) § 1-3 Noe kult, første ledd, tredje punktum": ["By design"]
            },
            "2021-12-30/2022-02-02": {
                "Min andre lov (versjon 2) § 1-4 Noe kult, første ledd, fjerde punktum": ["Det var som bare.."]
            }
        }
        """.trimIndent()

        val serialized = objectMapper.writeValueAsString(lovanvendelser.somLøsning())
        JSONAssert.assertEquals(forventetJson, serialized, true)

        val deserialisert : Map<String, Any> = objectMapper.readValue(serialized)

        val reserialized = objectMapper.writeValueAsString(deserialisert)
        JSONAssert.assertEquals(forventetJson, reserialized, true)

    }

    private companion object {
        private val objectMapper = ObjectMapper()

        private val MinLov = object: Lov {
            override val id = "Min lov (versjon 1)"
        }
        private val MinAndreLov = object: Lov {
            override val id = "Min andre lov (versjon 2)"
        }
        private val Lovhenvisning1 = object: Lovhenvisning {
            override val lov = MinLov
            override val henvisning = "§ 1-2 Noe kult, første ledd, andre punktum"
        }
        private val Lovhenvisning2 = object: Lovhenvisning {
            override val lov = MinLov
            override val henvisning = "§ 1-3 Noe kult, første ledd, tredje punktum"
        }
        private val Lovhenvisning3 = object: Lovhenvisning {
            override val lov = MinAndreLov
            override val henvisning = "§ 1-4 Noe kult, første ledd, fjerde punktum"
        }
    }
}