package no.nav.omsorgspenger.personopplysninger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.rivers.HentLøsning
import no.nav.omsorgspenger.rivers.LeggTilBehov

internal abstract class HentPersonopplysningerMelding<Personopplysninger, Fellesopplysninger>(
    private val defaultAttributter: Set<String>,
    private val måFinneAllePersoner: Boolean = false) :
    LeggTilBehov<HentPersonopplysningerInput>,
    HentLøsning<HentPersonopplysningerMelding.HentetPersonopplysninger<Personopplysninger, Fellesopplysninger>> {

    internal companion object {
        internal const val HentPersonopplysninger = "HentPersonopplysninger"
        private const val PersonopplysningerKey = "@løsninger.$HentPersonopplysninger.personopplysninger"
        private const val FellesopplysningerKey = "@løsninger.$HentPersonopplysninger.fellesopplysninger"
    }

    override fun behov(behovInput: HentPersonopplysningerInput) = Behov(
        navn = HentPersonopplysninger,
        input = mapOf(
            "måFinneAllePersoner" to måFinneAllePersoner,
            "identitetsnummer" to behovInput.identitetsnummer,
            "attributter" to when (behovInput.attributter.isEmpty()) {
                true -> defaultAttributter
                false -> behovInput.attributter
            }
        )
    )

    override fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(PersonopplysningerKey, FellesopplysningerKey)
    }

    override fun hentLøsning(packet: JsonMessage): HentetPersonopplysninger<Personopplysninger, Fellesopplysninger> {
        val personopplysninger = (packet[PersonopplysningerKey] as ObjectNode)
            .fields()
            .asSequence()
            .map { it.key to (it.value as ObjectNode) }
            .toMap()

        return HentetPersonopplysninger(
            personopplysninger = mapPersonopplysninger(personopplysninger),
            fellesopplysninger = mapFellesopplysninger(packet[FellesopplysningerKey])
        )

    }

    abstract fun mapPersonopplysninger(input: Map<Identitetsnummer, ObjectNode>) : Map<Identitetsnummer, Personopplysninger>
    abstract fun mapFellesopplysninger(input: JsonNode) : Fellesopplysninger

    internal data class HentetPersonopplysninger<Personopplysninger, Fellesopplysninger>(
        internal val personopplysninger: Map<Identitetsnummer, Personopplysninger>,
        internal val fellesopplysninger: Fellesopplysninger
    )
}

internal data class HentPersonopplysningerInput(
    val identitetsnummer: Set<Identitetsnummer>,
    val attributter: Set<String> = emptySet()
)