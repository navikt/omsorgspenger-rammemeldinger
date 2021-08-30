package no.nav.omsorgspenger.aleneom

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import javax.sql.DataSource

internal class AleneOmOmsorgenRepository(
    private val dataSource: DataSource) {

    internal fun hent(
        saksnummer: Saksnummer
    ) : Set<AleneOmOmsorgen> {
        return using(sessionOf(dataSource)) { session ->
            session.run(hentQuery(saksnummer = saksnummer).map { row ->
                row.somAleneOmOmsorgen()
            }.asList)
        }.toSet()
    }

    private companion object {
        private const val HentStatement = """
            SELECT * FROM aleneomomsorgen WHERE saksnummer = :saksnummer AND status = :status
        """

        private fun hentQuery(saksnummer: Saksnummer) = queryOf(HentStatement, mapOf(
            "saksnummer" to saksnummer,
            "status" to "Aktiv"
        ))

        private fun Row.somAleneOmOmsorgen() = AleneOmOmsorgen(
            registrert = zonedDateTime("registrert"),
            periode = Periode(
                fom = localDate("fom"),
                tom = localDate("tom")
            ),
            barn = AleneOmOmsorgen.Barn(
                identitetsnummer = string("barn_identitetsnummer"),
                fødselsdato = localDate("barn_fodselsdato")
            ),
            behovssekvensId = string("behovssekvens_id"),
            regstrertIForbindelseMed = string("registrert_ifbm")
        )
    }
}