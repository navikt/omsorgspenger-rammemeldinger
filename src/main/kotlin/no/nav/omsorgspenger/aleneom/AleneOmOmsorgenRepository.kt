package no.nav.omsorgspenger.aleneom

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgspenger.BehovssekvensId
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import javax.sql.DataSource

internal class AleneOmOmsorgenRepository(
    private val dataSource: DataSource) {

    internal enum class RegistreresIForbindelseMed {
        Overføring
    }

    internal fun lagre(
        saksnummer: Saksnummer,
        behovssekvensId: BehovssekvensId,
        registreresIForbindelseMed: RegistreresIForbindelseMed,
        aleneOmOmsorgenFor: Set<AleneOmOmsorgenFor>) = using(sessionOf(dataSource)) { session ->
        aleneOmOmsorgenFor.forEach {
            session.run(lagreQuery(
                saksnummer = saksnummer,
                behovssekvensId = behovssekvensId,
                registreresIForbindelseMed = registreresIForbindelseMed,
                aleneOmOmsorgenFor = it
            ).asUpdate)
        }
    }

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

        private const val LagreStatement = """
            INSERT INTO aleneomomsorgen 
                (saksnummer, fom, tom, barn_identitetsnummer, barn_fodselsdato, behovssekvens_id, registrert_ifbm)
            VALUES
                (:saksnummer, :fom, :tom, :barn_identitetsnummer, :barn_fodselsdato, :behovssekvens_id, :registrert_ifbm)
            ON CONFLICT DO NOTHING
        """

        private fun lagreQuery(
            saksnummer: Saksnummer,
            behovssekvensId: BehovssekvensId,
            aleneOmOmsorgenFor: AleneOmOmsorgenFor,
            registreresIForbindelseMed: RegistreresIForbindelseMed
        ) = queryOf(LagreStatement, mapOf(
            "saksnummer" to saksnummer,
            "fom" to aleneOmOmsorgenFor.aleneOmOmsorgenI.fom,
            "tom" to aleneOmOmsorgenFor.aleneOmOmsorgenI.tom,
            "barn_identitetsnummer" to aleneOmOmsorgenFor.identitetsnummer,
            "barn_fodselsdato" to aleneOmOmsorgenFor.fødselsdato,
            "behovssekvens_id" to behovssekvensId,
            "registrert_ifbm" to registreresIForbindelseMed.name
        ))

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