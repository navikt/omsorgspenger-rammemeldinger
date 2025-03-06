package no.nav.omsorgspenger.testutils

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.awaitility.Awaitility
import org.json.JSONObject
import java.time.Duration

internal fun TestRapid.ventPå(antallMeldinger: Int) =
    Awaitility.await().atMost(Duration.ofSeconds(1)).until { inspektør.size == antallMeldinger }

internal fun TestRapid.sisteMelding() =
    inspektør.message(inspektør.size - 1).toString()

internal fun String.somJsonMessage() =
    JsonMessage(toString(), MessageProblems(this), SimpleMeterRegistry()).also { it.interestedIn("@løsninger") }

internal fun TestRapid.sisteMeldingSomJsonMessage() =
    sisteMelding().somJsonMessage()

internal fun TestRapid.sisteMeldingSomJSONObject() =
    JSONObject(sisteMelding())

internal fun TestRapid.printSisteMelding() =
    println(sisteMeldingSomJSONObject().toString(1))