package no.nav.omsorgspenger.behovssekvens

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgspenger.BehovssekvensId
import javax.sql.DataSource

internal typealias Behovssekvens = String

internal class BehovssekvensRepository(
    private val dataSource: DataSource) {

    internal fun skalHåndtere(
        behovssekvensId: BehovssekvensId,
        steg: String) : Boolean {
        return using(sessionOf(dataSource)) { session ->
            session.run(henteQuery(
                behovssekvensId = behovssekvensId,
                steg = steg
            ).map { it.long("id") }.asList).size
        } == 0
    }

    internal fun harHåndtert(
        behovssekvensId: BehovssekvensId,
        behovssekvens: Behovssekvens,
        steg: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(lagreQuery(
                behovssekvensId = behovssekvensId,
                behovssekvens = behovssekvens,
                steg = steg
            ).asUpdate)
        }
    }

    private companion object {
        private const val HentStatement = """
            SELECT id FROM behovssekvens 
            WHERE behovssekvens_id = :behovssekvens_id 
            AND gjennomfort_steg = :gjennomfort_steg
        """

        private fun henteQuery(
            behovssekvensId: BehovssekvensId,
            steg: String) = queryOf(HentStatement, mapOf(
            "behovssekvens_id" to behovssekvensId,
            "gjennomfort_steg" to steg
        ))

        private const val LagreStatement = """
            INSERT INTO behovssekvens 
                (behovssekvens_id, behovssekvens, gjennomfort_steg)
            VALUES
                (:behovssekvens_id, (to_json(:behovssekvens::json)), :gjennomfort_steg)
        """

        private fun lagreQuery(
            behovssekvensId: BehovssekvensId,
            behovssekvens: Behovssekvens,
            steg: String) = queryOf(LagreStatement, mapOf(
            "behovssekvens_id" to behovssekvensId,
            "behovssekvens" to behovssekvens,
            "gjennomfort_steg" to steg
        ))
    }
}