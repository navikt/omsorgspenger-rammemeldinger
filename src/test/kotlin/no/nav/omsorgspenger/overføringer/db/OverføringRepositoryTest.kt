package no.nav.omsorgspenger.overføringer.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgspenger.Kilde
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.lovverk.JobberINorge
import no.nav.omsorgspenger.lovverk.Lovanvendelser
import no.nav.omsorgspenger.overføringer.GjeldendeOverføring
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringFått
import no.nav.omsorgspenger.overføringer.GjeldendeOverføringGitt
import no.nav.omsorgspenger.testutils.DataSourceExtension
import no.nav.omsorgspenger.testutils.cleanAndMigrate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.sql.DataSource

@ExtendWith(DataSourceExtension::class)
internal class OverføringRepositoryTest(
    private val dataSource: DataSource
) {
    private val repository = OverføringRepository(dataSource.cleanAndMigrate())

    @Test
    fun `henter aktive overføringer med historiske kilder og lovanvendelser`() {
        val overføringId = leggInnOverføring(Ola, Kari)
        leggInnLogg(overføringId, "1", "Opprettet")
        leggInnLogg(overføringId, "2", "Endret til og med til 2020-11-30")
        leggInnLogg(leggInnOverføring(Trond, Kari), "3", "Opprettet")

        val overføringer = repository.hentAktiveOverføringer(setOf(Ola))
        val kilder = setOf(
            Kilde.internKilde("1", "Overføring"),
            Kilde.internKilde("2", "Overføring")
        )

        assertThat(overføringer.keys).containsExactlyInAnyOrder(Ola, Kari)
        assertThat(overføringer.getValue(Ola).fått).isEmpty()
        assertThat(overføringer.getValue(Kari).gitt).isEmpty()
        assertThat(overføringer.getValue(Ola).gitt.map {
            it.copy(gjennomført = it.gjennomført.withZoneSameInstant(ZoneOffset.UTC))
        }).containsExactly(GjeldendeOverføringGitt(
            gjennomført = gjennomført,
            antallDager = 7,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            kilder = kilder,
            lovanvendelser = lovanvendelser,
            til = Kari,
            antallDagerØnsketOverført = 10
        ))
        assertThat(overføringer.getValue(Kari).fått.map {
            it.copy(gjennomført = it.gjennomført.withZoneSameInstant(ZoneOffset.UTC))
        }).containsExactly(GjeldendeOverføringFått(
            gjennomført = gjennomført,
            antallDager = 7,
            periode = periode,
            status = GjeldendeOverføring.Status.Aktiv,
            kilder = kilder,
            lovanvendelser = lovanvendelser,
            fra = Ola
        ))
    }

    @Test
    fun `henter overføringer der etterspurte saksnummer er giver eller mottaker`() {
        leggInnLogg(leggInnOverføring(Ola, Kari), "1", "Opprettet")
        leggInnLogg(leggInnOverføring(Trond, Ola), "2", "Opprettet")
        leggInnLogg(leggInnOverføring(Trond, Kari), "3", "Opprettet")

        val overføringer = repository.hentAktiveOverføringer(setOf(Ola))

        assertThat(overføringer.keys).containsExactlyInAnyOrder(Ola, Kari, Trond)
        assertThat(overføringer.getValue(Ola).gitt.map { it.til }).containsExactly(Kari)
        assertThat(overføringer.getValue(Ola).fått.map { it.fra }).containsExactly(Trond)
        assertThat(overføringer.getValue(Kari).fått.map { it.fra }).containsExactly(Ola)
        assertThat(overføringer.getValue(Trond).gitt.map { it.til }).containsExactly(Ola)
        assertThat(overføringer.getValue(Kari).gitt).isEmpty()
        assertThat(overføringer.getValue(Trond).fått).isEmpty()
        assertThat(overføringer.getValue(Ola).gitt.single().kilder)
            .containsExactly(Kilde.internKilde("1", "Overføring"))
        assertThat(overføringer.getValue(Ola).fått.single().kilder)
            .containsExactly(Kilde.internKilde("2", "Overføring"))

        val flereSaksnummer = repository.hentAktiveOverføringer(setOf(Ola, Kari))
        assertThat(flereSaksnummer.getValue(Kari).fått.map { it.fra })
            .containsExactlyInAnyOrder(Ola, Trond)
    }

    @Test
    fun `utelater inaktive overføringer og beholder aktive uten logg`() {
        leggInnOverføring(Ola, Kari)
        leggInnOverføring(Ola, Trond, GjeldendeOverføring.Status.Deaktivert)
        leggInnOverføring(Trond, Ola, GjeldendeOverføring.Status.Avslått)

        val overføringer = repository.hentAktiveOverføringer(setOf(Ola))

        assertThat(overføringer.keys).containsExactlyInAnyOrder(Ola, Kari)
        assertThat(overføringer.getValue(Ola).gitt).hasSize(1)
        assertThat(overføringer.getValue(Kari).fått).hasSize(1)
        assertThat(overføringer.getValue(Ola).fått).isEmpty()
        assertThat(overføringer.getValue(Kari).gitt).isEmpty()
        assertThat(overføringer.getValue(Ola).gitt.single().kilder).isEmpty()
        assertThat(overføringer.getValue(Kari).fått.single().kilder).isEmpty()
    }

    private fun leggInnOverføring(
        fra: Saksnummer,
        til: Saksnummer,
        status: GjeldendeOverføring.Status = GjeldendeOverføring.Status.Aktiv
    ): Long = using(sessionOf(dataSource)) { session ->
        requireNotNull(session.run(queryOf(
            """
                INSERT INTO overforing
                    (gjennomfort, fom, tom, fra, til, antall_dager,
                     antall_dager_onsket_overfort, status, lovanvendelser)
                VALUES
                    (:gjennomfort, :fom, :tom, :fra, :til, 7, 10, :status, :lovanvendelser::jsonb)
                RETURNING id
            """.trimIndent(),
            mapOf(
                "gjennomfort" to gjennomført.toOffsetDateTime(),
                "fom" to periode.fom,
                "tom" to periode.tom,
                "fra" to fra,
                "til" to til,
                "status" to status.name,
                "lovanvendelser" to lovanvendelser.somJson()
            )
        ).map { it.long("id") }.asSingle))
    }

    private fun leggInnLogg(overføringId: Long, behovssekvensId: String, melding: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf(
                """
                    INSERT INTO overforing_logg (overforing_id, behovssekvens_id, melding)
                    VALUES (:overforing_id, :behovssekvens_id, :melding)
                """.trimIndent(),
                mapOf(
                    "overforing_id" to overføringId,
                    "behovssekvens_id" to behovssekvensId,
                    "melding" to melding
                )
            ).asUpdate)
        }
    }

    private companion object {
        private const val Ola = "Ola"
        private const val Kari = "Kari"
        private const val Trond = "Trond"
        private val gjennomført = ZonedDateTime.parse("2020-11-01T12:15:00Z")
        private val periode = Periode("2020-11-01/2020-11-30")
        private val lovanvendelser = Lovanvendelser().leggTil(
            periode = periode,
            lovhenvisning = JobberINorge,
            anvendelse = "Tester repository"
        )
    }
}
