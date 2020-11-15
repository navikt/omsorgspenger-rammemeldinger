package no.nav.omsorgspenger.behovssekvens

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

internal class BehovssekvensRepository(
    private val dataSource: DataSource) {

    internal fun skalHåndtere(
        behovssekvensId: BehovssekvensId,
        steg: String) : Boolean {
        return using(sessionOf(dataSource)) { session ->
            session.run(hentSisteIdQuery(
                behovssekvensId = behovssekvensId,
                steg = steg
            ).map { it.long("id") }.asSingle)
        } == null
    }

    internal fun harHåndtert(
        behovssekvensId: BehovssekvensId,
        behovssekvens: BehovssekvensJSON,
        steg: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(lagreQuery(
                behovssekvensId = behovssekvensId,
                behovssekvens = behovssekvens,
                steg = steg
            ).asUpdate)
        }
    }

    internal fun hent(behovssekvensId: BehovssekvensId) : List<Behovssekvens> {
        return using(sessionOf(dataSource)) { session ->
            session.run(hentQuery(
                behovssekvensId = behovssekvensId,
            ).map { row -> row.somBehovssekvens() }.asList)
        }
    }

    private companion object {
        private const val HentSisteIdStatement = """
            SELECT id FROM behovssekvens 
            WHERE behovssekvens_id = :behovssekvens_id 
            AND gjennomfort_steg = :gjennomfort_steg
            ORDER BY id DESC LIMIT 1
        """

        private fun hentSisteIdQuery(
            behovssekvensId: BehovssekvensId,
            steg: String) = queryOf(HentSisteIdStatement, mapOf(
            "behovssekvens_id" to behovssekvensId,
            "gjennomfort_steg" to steg
        ))

        private const val HentStatement = """
            SELECT * FROM behovssekvens 
            WHERE behovssekvens_id = :behovssekvens_id
        """

        private fun hentQuery(
            behovssekvensId: BehovssekvensId) = queryOf(HentStatement, mapOf(
            "behovssekvens_id" to behovssekvensId
        ))

        private const val LagreStatement = """
            INSERT INTO behovssekvens 
                (behovssekvens_id, behovssekvens, gjennomfort_steg)
            VALUES
                (:behovssekvens_id, (to_json(:behovssekvens::json)), :gjennomfort_steg)
        """

        private fun lagreQuery(
            behovssekvensId: BehovssekvensId,
            behovssekvens: BehovssekvensJSON,
            steg: String) = queryOf(LagreStatement, mapOf(
            "behovssekvens_id" to behovssekvensId,
            "behovssekvens" to behovssekvens,
            "gjennomfort_steg" to steg
        ))

        private fun Row.somBehovssekvens() = Behovssekvens(
            gjennomført = zonedDateTime("gjennomfort"),
            gjennomførtSteg = string("gjennomfort_steg"),
            behovssekvensId = string("behovssekvens_id"),
            behovssekvens = string("behovssekvens")
        )
    }
}