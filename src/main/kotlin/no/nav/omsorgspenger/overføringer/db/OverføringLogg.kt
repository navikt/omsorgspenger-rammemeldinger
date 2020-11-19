package no.nav.omsorgspenger.overføringer.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import java.sql.Array

internal object OverføringLogg {
    internal fun Session.overføringerOpprettet(
        behovssekvensId: BehovssekvensId,
        overføringIder: List<Long>,
    ) = overføringIder.forEach {
        update(leggTilLoggQuery(
            behovssekvensId = behovssekvensId,
            overføringId = it,
            melding = "Overføring opprettet"
        ))
    }

    internal fun Session.overføringerEndret(
        behovssekvensId: BehovssekvensId,
        overføringIder: List<Long>,
        endring: String
    ) = overføringIder.forEach {
        update(leggTilLoggQuery(
            behovssekvensId = behovssekvensId,
            overføringId = it,
            melding = endring
        ))
    }

    internal fun Session.hentOverføringLogger(
        overføringIder: Array
    ) = run(hentLoggQuery(overføringIder).map { row ->
        OverføringLoggDb(
            behovssekvensId = row.string("behovssekvens_id"),
            overføringId = row.long("overforing_id"),
            melding = row.string("melding")
        )
    }.asList)

    internal data class OverføringLoggDb(
        internal val behovssekvensId: BehovssekvensId,
        internal val overføringId: Long,
        internal val melding: String) {
    }

    private const val LeggTilLoggStatement =
        """
        INSERT INTO overforing_logg (overforing_id, behovssekvens_id, melding) 
        VALUES(:overforing_id, :behovssekvens_id, :melding)
        """

    private fun leggTilLoggQuery(
        behovssekvensId: BehovssekvensId,
        overføringId: Long,
        melding: String
    ) = queryOf(LeggTilLoggStatement, mapOf(
        "overforing_id" to overføringId,
        "behovssekvens_id" to behovssekvensId,
        "melding" to melding
    ))

    private const val HentLoggStatement =
        """
        SELECT * from overforing_logg 
        WHERE overforing_id = ANY(:overforing_ids)
        """

    private fun hentLoggQuery(
        overføringIder: Array
    ) = queryOf(HentLoggStatement, mapOf(
        "overforing_ids" to overføringIder
    ))
}