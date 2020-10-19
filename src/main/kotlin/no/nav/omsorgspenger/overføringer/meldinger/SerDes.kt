package no.nav.omsorgspenger.overføringer.meldinger

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.omsorgspenger.Periode

internal object SerDes {

    internal val JacksonObjectMapper = jacksonObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModule(SimpleModule().also { simpleModule ->
            simpleModule.addSerializer(Periode::class.java, PeriodeSerializer())
            simpleModule.addDeserializer(Periode::class.java, PeriodeDeserializer())
        })

    private class PeriodeSerializer : JsonSerializer<Periode>() {
        override fun serialize(periode: Periode, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
            jsonGenerator.writeString(periode.toString())
        }
    }
    private class PeriodeDeserializer : JsonDeserializer<Periode>() {
        override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Periode {
            return Periode(jsonParser.valueAsString)
        }
    }
}


