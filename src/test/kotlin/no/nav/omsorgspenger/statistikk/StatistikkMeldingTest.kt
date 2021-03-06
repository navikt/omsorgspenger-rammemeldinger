package no.nav.omsorgspenger.statistikk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import no.nav.omsorgspenger.personopplysninger.Enhet
import no.nav.omsorgspenger.personopplysninger.Enhetstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

internal class StatistikkMeldingTest {
    val schema = TestSchemaHelper.schema
    val objectMapper = TestSchemaHelper.objectMapper

    @Test
    fun `melding er komplett med alle felt`() {
        assertThat(melding).hasNoNullFieldsOrPropertiesExcept("behandlendeEnhetKode","behandlendeEnhetType")
    }

    @Test
    fun `melding validerer mot schema`() {
        val node = objectMapper.readTree(melding.toJson())
        val errors = schema.validate(node)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `melding kan parses`() {
        val res = StatistikkMelding.fromJson(melding.toJson())
        assertThat(res).isEqualTo(melding)
    }

    private val melding = StatistikkMelding.instance(
        saksnummer = "1",
        behovssekvensId = "2",
        mottaksdato = LocalDate.parse("2020-01-01"),
        registreringsdato = LocalDate.parse("2020-01-01"),
        behandlingType = "type",
        behandlingResultat = "resultat",
        undertype = "under",
        mottatt = ZonedDateTime.now(),
        aktørId = "12345678",
        enhet = Enhet(
            enhetstype = Enhetstype.VANLIG,
            enhetsnummer = "1234"
        )
    )
}

object TestSchemaHelper {
    val objectMapper = ObjectMapper()

    val schema: JsonSchema
        get() = schema(get("behandling_schema.json"))

    private fun get(name: String): String {
        return javaClass.getResourceAsStream(name)
                .readAllBytes()
                .toString(Charsets.UTF_8)
    }

    private fun schema(schemaAsString: String): JsonSchema {
        val schemaNode = objectMapper.readTree(schemaAsString)
        disallowAdditionalProperties(schemaNode)
        return JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).build().getSchema(schemaNode)
    }

    // Schemavalideringsbiblioteket har ikke støtte for å globalt validere at det ikke finnes ukjente properties
    // i JSON-objektet. Bruker derfor denne metoden til å modifisere schema slik at ukjente properties ikke er lov
    // når testen kjører.
    private fun disallowAdditionalProperties(node: JsonNode) {
        if(node.isObject) {
            val typeNode = node.get("type")
            if(typeNode != null && typeNode.textValue() == "object") {
                val objectNode = node as ObjectNode
                objectNode.put("additionalProperties", false)
            }

            for(field in node.fields()) {
                disallowAdditionalProperties(field.value)
            }
        }
        if(node.isArray) {
            for(elementNode in node.elements()) {
                disallowAdditionalProperties(elementNode)
            }
        }
    }
}