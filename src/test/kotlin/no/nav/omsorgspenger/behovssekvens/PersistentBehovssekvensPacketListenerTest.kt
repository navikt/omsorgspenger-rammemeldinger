package no.nav.omsorgspenger.behovssekvens

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
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
    private val messageContextMock = mockk<RapidsConnection.MessageContext>().also {
        every { it.send(any(), any()) }.returns(Unit)
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

        val jsonMessage1 = "1".somJsonMessage()

        for (i in 0..10) {
            packetListener1.onPacket(jsonMessage1, messageContextMock)
        }

        gjortNganger(1)

        for (i in 0..10) {
            packetListener2.onPacket(jsonMessage1, messageContextMock)
        }

        gjortNganger(2)

        val jsonMessage2 = "2".somJsonMessage()
        packetListener1.onPacket(jsonMessage2, messageContextMock)
        gjortNganger(3)
        packetListener2.onPacket(jsonMessage2, messageContextMock)
        gjortNganger(4)
    }

    private fun gjortNganger(n: Int) {
        verify(exactly = n) { workMock.doWork() }
        verify(exactly = n) { messageContextMock.send(any(), any())}
    }

    private companion object {
        private fun String.somJsonMessage() = """
            {
                "@id": "$this",
                "@correlationId": "2"
            }
        """.trimIndent().let { JsonMessage(
            originalMessage = it,
            problems = MessageProblems(originalMessage = it)
        ).also { jsonMessage ->
            jsonMessage.interestedIn("@id","@correlationId")
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