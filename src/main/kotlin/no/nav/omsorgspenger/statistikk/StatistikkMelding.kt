package no.nav.omsorgspenger.statistikk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StatistikkMelding(
        @JsonProperty("saksnummer")
        val saksnummer: String,

        @JsonProperty("behandlingId")
        val behandlingId: String,

        @JsonProperty("mottattDato")
        val mottattDato: LocalDate,

        @JsonProperty("registrertDato")
        val registrertDato: LocalDate,

        @JsonProperty("behandlingType")
        val behandlingType: String,

        @JsonProperty("behandlingStatus")
        val behandlingStatus: String,

        @JsonProperty("funksjonellTid")
        val funksjonellTid: OffsetDateTime,

        @JsonProperty("aktorId")
        val aktorId: String,

        @JsonProperty("tekniskTid")
        val tekniskTid: OffsetDateTime,

        @JsonProperty("sakId")
        val sakId: String = saksnummer,

        @JsonProperty("ytelseType")
        val ytelseType: String = "omsorgspenger",

        @JsonProperty("ansvarligEnhetKode")
        val ansvarligEnhetKode: String,

        @JsonProperty("ansvarligEnhetType")
        val ansvarligEnhetType: String = "NORG",

        @JsonProperty("behandlendeEnhetKode")
        val behandlendeEnhetKode: String? = null,

        @JsonProperty("behandlendeEnhetType")
        val behandlendeEnhetType: String? = null,

        @JsonProperty("totrinnsbehandling")
        val totrinnsbehandling: Boolean = false,

        @JsonProperty("avsender")
        val avsender: String = "omsorgspenger-rammemeldinger",

        @JsonProperty("versjon")
        val versjon: Long = 1L

) {
    fun toJson(): String {
        return objectMapper.writeValueAsString(this)
    }

    fun utenSkjermedeFelt(): StatistikkMelding {
        return copy(
                ansvarligEnhetKode = "-5",
                behandlendeEnhetKode = "-5",
                aktorId = "-5"
        )
    }

    companion object {
        fun fromJson(json: String): StatistikkMelding {
            return objectMapper.readValue(json)
        }

        private val objectMapper: ObjectMapper by lazy {
            val om = ObjectMapper()
            om.registerModule(JavaTimeModule())
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            om.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            om
        }
    }
}