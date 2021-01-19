package no.nav.omsorgspenger.statistikk

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.personopplysninger.Enhet
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StatistikkMelding @JsonCreator private constructor(
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

    @JsonProperty("tekniskTid")
    val tekniskTid: OffsetDateTime,

    @JsonProperty("aktorId")
    val aktorId: String,

    @JsonProperty("ansvarligEnhetKode")
    val ansvarligEnhetKode: String,

    @JsonProperty("ansvarligEnhetType")
    val ansvarligEnhetType: String,

    @JsonProperty("behandlendeEnhetKode")
    val behandlendeEnhetKode: String?,

    @JsonProperty("behandlendeEnhetType")
    val behandlendeEnhetType: String?,

    @JsonProperty("versjon")
    val versjon: Long,

    @JsonProperty("avsender")
    val avsender: String,

    @JsonProperty("totrinnsbehandling")
    val totrinnsbehandling: Boolean,

    @JsonProperty("ytelseType")
    val ytelseType: String,

    @JsonProperty("underType")
    val underType: String) {

    internal fun toJson() = objectMapper.writeValueAsString(this)

    internal companion object {
        internal fun instance(
            saksnummer: Saksnummer,
            behovssekvensId: BehovssekvensId,
            aktørId: AktørId,
            registreringsdato: LocalDate,
            mottaksdato: LocalDate,
            mottatt: ZonedDateTime,
            behandlingType: String,
            behandlingStatus: String,
            undertype: String,
            enhet: Enhet) = StatistikkMelding(
            saksnummer = saksnummer,
            behandlingId = behovssekvensId,
            registrertDato = registreringsdato,
            mottattDato = mottaksdato,
            funksjonellTid = mottatt.toOffsetDateTime(),
            aktorId = when (enhet.skjermet) {
                true -> SkjermetVerdi
                false -> aktørId
            },
            ansvarligEnhetKode = enhet.enhetsnummer,
            behandlingStatus = behandlingStatus,
            behandlingType = behandlingType,
            underType = undertype,
            tekniskTid = OffsetDateTime.now(),
            // Statiske verdier
            versjon = 2L,
            ytelseType = "omsorgspenger",
            avsender = "omsorgspenger-rammemeldinger",
            ansvarligEnhetType = "NORG",
            behandlendeEnhetType = null,
            behandlendeEnhetKode = null,
            totrinnsbehandling = false
        )

        private const val SkjermetVerdi = "-5"

        internal fun fromJson(json: String): StatistikkMelding {
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