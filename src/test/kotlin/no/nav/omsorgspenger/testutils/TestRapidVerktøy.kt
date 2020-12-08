package no.nav.omsorgspenger.testutils

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.awaitility.Awaitility
import org.json.JSONObject
import java.time.Duration

internal fun TestRapid.ventPå(antallMeldinger: Int) =
    Awaitility.await().atMost(Duration.ofSeconds(1)).until { inspektør.size == antallMeldinger }

internal fun TestRapid.sisteMelding() =
    inspektør.message(inspektør.size - 1).toString()

internal fun String.somJsonMessage() =
    JsonMessage(toString(), MessageProblems(this)).also { it.interestedIn("@løsninger") }

internal fun TestRapid.sisteMeldingSomJsonMessage() =
    sisteMelding().somJsonMessage()

internal fun TestRapid.sisteMeldingSomJSONObject() =
    JSONObject(sisteMelding())