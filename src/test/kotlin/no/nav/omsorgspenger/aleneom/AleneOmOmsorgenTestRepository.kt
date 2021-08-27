package no.nav.omsorgspenger.aleneom

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import javax.sql.DataSource

internal class AleneOmOmsorgenTestRepository(
    private val dataSource: DataSource) {

    internal enum class RegistreresIForbindelseMed {
        Overføring,
        KoronaOverføring,
        AleneOmOmsorgen
    }


    internal fun lagre(
        saksnummer: Saksnummer,
        behovssekvensId: BehovssekvensId,
        registreresIForbindelseMed: RegistreresIForbindelseMed,
        aleneOmOmsorgenFor: Set<AleneOmOmsorgenFor>) = using(sessionOf(dataSource)) { session ->
        aleneOmOmsorgenFor.forEach {
            session.run(
                lagreQuery(
                saksnummer = saksnummer,
                behovssekvensId = behovssekvensId,
                registreresIForbindelseMed = registreresIForbindelseMed,
                aleneOmOmsorgenFor = it
            ).asUpdate)
        }
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
        ) = queryOf(
            LagreStatement, mapOf(
                "saksnummer" to saksnummer,
                "fom" to aleneOmOmsorgenFor.aleneOmOmsorgenI.fom,
                "tom" to aleneOmOmsorgenFor.aleneOmOmsorgenI.tom,
                "barn_identitetsnummer" to aleneOmOmsorgenFor.identitetsnummer,
                "barn_fodselsdato" to aleneOmOmsorgenFor.fødselsdato,
                "behovssekvens_id" to behovssekvensId,
                "registrert_ifbm" to registreresIForbindelseMed.name
            )
        )
    }
}
