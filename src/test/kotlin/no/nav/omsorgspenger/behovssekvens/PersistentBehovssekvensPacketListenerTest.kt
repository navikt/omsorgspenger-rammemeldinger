package no.nav.omsorgspenger.behovssekvens

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import de.huxhorn.sulky.ulid.ULID
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class PersistentBehovssekvensPacketListenerTest(
    dataSource: DataSource){

    private val workMock = mockk<Work>().also {
        every { it.doWork() }.returns(Unit)
    }
    private val messageContextMock = mockk<MessageContext>().also {
        every { it.publish(any(), any()) }.returns(Unit)
    }
    private val packetListener1 = WorkPacketListener(
        dataSource = dataSource.cleanAndMigrate(),
        work = workMock,
        steg = "Work1"
    )

    private val packetListener2 = WorkPacketListener(
        dataSource = dataSource.cleanAndMigrate(),
        work = workMock,
        steg = "Work2"
    )

    @Test
    fun `Test at vi kun håndterer en gang`() {

        val jsonMessage1 = ULID().nextULID().somJsonMessage()

        for (i in 0..10) {
            packetListener1.onPacket(jsonMessage1, messageContextMock, MessageMetadata("", -1, -1, null, emptyMap()), SimpleMeterRegistry())
        }

        gjortNganger(1)

        for (i in 0..10) {
            packetListener2.onPacket(jsonMessage1, messageContextMock, MessageMetadata("", -1, -1, null, emptyMap()), SimpleMeterRegistry())
        }

        gjortNganger(2)

        val jsonMessage2 = ULID().nextULID().somJsonMessage()
        packetListener1.onPacket(jsonMessage2, messageContextMock, MessageMetadata("", -1, -1, null, emptyMap()), SimpleMeterRegistry())
        gjortNganger(3)
        packetListener2.onPacket(jsonMessage2, messageContextMock, MessageMetadata("", -1, -1, null, emptyMap()), SimpleMeterRegistry())
        gjortNganger(4)
    }

    private fun gjortNganger(n: Int) {
        verify(exactly = n) { workMock.doWork() }
        verify(exactly = n) { messageContextMock.publish(any(), any())}
    }

    private companion object {
        private fun String.somJsonMessage() = """
            {
                "@behovssekvensId": "$this",
                "@correlationId": "2"
            }
        """.trimIndent().let { JsonMessage(
            originalMessage = it,
            problems = MessageProblems(originalMessage = it),
            metrics = SimpleMeterRegistry(),
            randomIdGenerator = null
        ).also { jsonMessage ->
            jsonMessage.interestedIn("@correlationId","@behovssekvensId")
        }}

        private class WorkPacketListener(
            dataSource: DataSource,
            private val work: Work,
            steg: String
        ) : PersistentBehovssekvensPacketListener(
            logger = LoggerFactory.getLogger(PersistentBehovssekvensPacketListenerTest::class.java),
            steg = steg,
            behovssekvensRepository = BehovssekvensRepository(dataSource)) {
            override fun handlePacket(id: String, packet: JsonMessage): Boolean {
                return work.doWork().let { true }
            }
        }

        private interface Work{
            fun doWork()
        }
    }

}