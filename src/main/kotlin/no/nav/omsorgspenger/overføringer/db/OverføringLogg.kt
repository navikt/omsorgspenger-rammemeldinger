package no.nav.omsorgspenger.overføringer.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId

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
}