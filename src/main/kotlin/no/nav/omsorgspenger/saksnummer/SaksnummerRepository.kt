package no.nav.omsorgspenger.saksnummer

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer
import java.sql.Array
import javax.sql.DataSource

internal class SaksnummerRepository(
    private val dataSource: DataSource) {

    internal fun lagreMapping(mapping: Map<Identitetsnummer, Saksnummer>) {
        sessionOf(dataSource).use { session ->
            mapping.forEach { (identitetsnummer, saksnummer) ->
                session.run(leggTilMappingQuery(
                    identitetsnummer = identitetsnummer,
                    saksnummer = saksnummer
                ).asUpdate)
            }
        }
    }

    internal fun hentSisteMappingFor(saksnummer: Set<Saksnummer>) : Map<Identitetsnummer, Saksnummer> {
        val mapping = mutableMapOf<Identitetsnummer, Saksnummer>()
        sessionOf(dataSource).use { session ->
            session.run(hentSisteMappingQuery(
                saksnummerArray = session.saksnummerArray(saksnummer)
            ).map { row ->
                mapping[row.string("identitetsnummer")] = row.string("sak")
            }.asList)
        }
        return mapping
    }

    internal fun hentSaksnummerFor(identitetsnummer: Identitetsnummer) : Saksnummer? {
        return sessionOf(dataSource).use { session ->
            session.run(hentSaksnummerFraIdentitetsnummerQuery(
                identitetsnummer = identitetsnummer
            ).map { row ->
                row.string("sak")
            }.asSingle)
        }
    }

    private fun Session.saksnummerArray(saksnummer: Set<Saksnummer>) = createArrayOf("varchar", saksnummer)

    private companion object {
        private const val LeggTilMappingStatement =
            "INSERT INTO saksnummer (identitetsnummer, sak) VALUES(?,?) ON CONFLICT DO NOTHING"
        private fun leggTilMappingQuery(identitetsnummer: Identitetsnummer, saksnummer: Saksnummer) =
            queryOf(LeggTilMappingStatement, identitetsnummer, saksnummer)

        private const val HentSisteMappingStatement =
            "SELECT DISTINCT ON(sak) sak, id, identitetsnummer FROM saksnummer WHERE sak = ANY(?) ORDER BY sak, id DESC"
        private fun hentSisteMappingQuery(saksnummerArray: Array) =
            queryOf(HentSisteMappingStatement, saksnummerArray)

        private const val HentSaksnummerFraIdentitetsnummerStatement =
            "SELECT id, sak FROM saksnummer WHERE identitetsnummer = ? ORDER BY id DESC LIMIT 1"
        private fun hentSaksnummerFraIdentitetsnummerQuery(identitetsnummer: Identitetsnummer) =
            queryOf(HentSaksnummerFraIdentitetsnummerStatement, identitetsnummer)
    }
}

internal fun Map<Identitetsnummer, Saksnummer>.identitetsnummer() = keys