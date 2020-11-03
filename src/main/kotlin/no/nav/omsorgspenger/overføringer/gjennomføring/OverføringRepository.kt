package no.nav.omsorgspenger.overføringer.gjennomføring

import kotliquery.Session
import kotliquery.sessionOf
import kotliquery.using
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.overføringer.Overføring
import no.nav.omsorgspenger.overføringer.fjernOverføringerUtenDager
import java.time.LocalDate
import javax.sql.DataSource

internal class OverføringRepository(
    private val dataSource: DataSource) {

    internal fun hentBerørteSaksnummer(
        fra: Saksnummer,
        til: Saksnummer,
        fraOgMed: LocalDate) : Set<Saksnummer> {
        return using(sessionOf(dataSource)) { session ->
            session.berørteOverføringer(
                fra = fra,
                til = til,
                fraOgMed = fraOgMed).saksnummer()
        }
    }

    internal fun hentOverføringer(
        saksnummer: Set<Saksnummer>
    ) : Map<Saksnummer, GjennomførteOverføringer> {
        return using(sessionOf(dataSource)) { session ->
            session.hentAlleOverføringer(
                saksnummer = saksnummer
            )
        }
    }

    internal fun gjennomførOverføringer(
        fra: Saksnummer,
        til: Saksnummer,
        overføringer: List<Overføring>) : Map<Saksnummer, GjennomførteOverføringer> {
        val overføringerMedDager = overføringer.fjernOverføringerUtenDager()
        val fraOgMed = overføringerMedDager.map { it.periode }.minByOrNull { it.fom }!!.fom
        return using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                val berørteOverføringer = transactionalSession.berørteOverføringer(
                    fra = fra,
                    til = til,
                    fraOgMed = fraOgMed
                )
                val overlapper = berørteOverføringer.overlapper(
                    fraOgMed = fraOgMed
                )

                transactionalSession.endrePeriodePåOverføringer(
                    overføringer = overlapper,
                    fraOgMed = fraOgMed
                )

                transactionalSession.deaktiverOverføringer(
                    overføringer = berørteOverføringer.minus(overlapper)
                )

                transactionalSession.lagreNyeOverføringer(
                    overføringer = overføringerMedDager
                )
                transactionalSession.hentAktiveOveføringer(
                    saksnummer = berørteOverføringer.saksnummer().plus(fra).plus(til)
                )
            }
        }
    }

    private fun Session.berørteOverføringer(
        fra: Saksnummer,
        til: Saksnummer,
        fraOgMed: LocalDate) : Set<OverføringDb> {
        /**
         *  Finne alle overføringer hvor 'fra' eller 'til' er en part
         *  og hvor 'tom' datoen er lik eller etter 'fraOgMed'
         */
        return setOf()
    }

    private fun Session.endrePeriodePåOverføringer(
        overføringer: Set<OverføringDb>,
        fraOgMed: LocalDate) {
    }

    private fun Session.deaktiverOverføringer(
        overføringer: Set<OverføringDb>) {
    }

    private fun Session.lagreNyeOverføringer(
        overføringer: List<Overføring>) {

    }

    private fun Session.hentAktiveOveføringer(
        saksnummer: Set<Saksnummer>
    ) : Map<Saksnummer, GjennomførteOverføringer> {
        return mapOf()
    }

    private fun Session.hentAlleOverføringer(
        saksnummer: Set<Saksnummer>
    ) : Map<Saksnummer, GjennomførteOverføringer> {
        return mapOf()
    }
}

private data class OverføringDb(
    val id: Long,
    val periode: Periode,
    val fra: Saksnummer,
    val til: Saksnummer
)

private fun Set<OverføringDb>.overlapper(fraOgMed: LocalDate) =
    filter { it.periode.inneholder(fraOgMed) }.toSet()
private fun Set<OverføringDb>.saksnummer() : Set<Saksnummer> =
    map { listOf(it.fra, it.til) }.flatten().toSet()