package no.nav.omsorgspenger.overføringer.db

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.omsorgspenger.Periode
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.extensions.erFørEllerLik
import org.slf4j.LoggerFactory
import java.sql.Array
import java.time.LocalDate

internal object OpphørOverføringer {
    internal data class OpphørteOverføringer(
        internal val deaktiverte: List<Long>,
        internal val nyTilOgMed: List<Long>
    )

    internal data class AktivOverføring(
        val id: Long,
        val periode: Periode
    )

    internal fun TransactionalSession.opphørOverføringer(
        tabell: String,
        aktiveOverføringer: List<AktivOverføring>,
        fraOgMed: LocalDate,
        onDeaktivert: (overføringIder: List<Long>) -> Unit = {},
        onNyTilOgMed: (overføringIder: List<Long>, nyTilOgMed: LocalDate) -> Unit = {_,_ ->}) : OpphørteOverføringer {
        require(tabell == "koronaoverforing" || tabell == "overforing"){
            "Støtter ikke opphøring av overføringer for Tabell=[$tabell]"
        }

        val nyTilOgMed = fraOgMed.fraOgMedTilNyTilOgMed()

        require(aktiveOverføringer.all { it.periode.tom.isAfter(nyTilOgMed) })

        val skalFåNyTilOgMed = aktiveOverføringer.filter { it.periode.fom.erFørEllerLik(nyTilOgMed) }
        val skalFåNyTilOgMedIds = skalFåNyTilOgMed.map { it.id }
        if (skalFåNyTilOgMed.isNotEmpty()) {
            logger.info("Oppdaterer tilOgMed på overføringer, Tabell=[$tabell], NyTilOgMed=[$nyTilOgMed], AntallOverføringer=[${skalFåNyTilOgMed.size}]")
            oppdaterTilOgMed(tabell = tabell, overføringer = skalFåNyTilOgMed, nyTilOgMed = nyTilOgMed)
            onNyTilOgMed(skalFåNyTilOgMedIds, nyTilOgMed)
        }


        val skalDeaktiveres = aktiveOverføringer.minus(skalFåNyTilOgMed)
        val skalDeaktiveresIds = skalDeaktiveres.map { it.id }
        if (skalDeaktiveres.isNotEmpty()) {
            logger.info("Deaktiverer overføringer, Tabell=[$tabell], AntallOverføringer=[${skalDeaktiveres.size}]")
            deaktivertOverføringer(tabell = tabell, overføringer = skalDeaktiveres)
            onDeaktivert(skalDeaktiveresIds)
        }

        return OpphørteOverføringer(
            deaktiverte = skalDeaktiveresIds,
            nyTilOgMed = skalFåNyTilOgMedIds
        )
    }

    internal fun TransactionalSession.opphørOverføringer(
        tabell: String,
        fra: Saksnummer,
        til: Saksnummer,
        fraOgMed: LocalDate,
        onDeaktivert: (overføringIder: List<Long>) -> Unit = {},
        onNyTilOgMed: (overføringIder: List<Long>, nyTilOgMed: LocalDate) -> Unit = {_,_ ->}) : OpphørteOverføringer {

        val aktiveOverføringer = hentAktiveOverføringer(
            tabell = tabell,
            fra = fra,
            til = til,
            tilOgMedEtter = fraOgMed.fraOgMedTilNyTilOgMed()
        )

        return opphørOverføringer(
            tabell = tabell,
            fraOgMed = fraOgMed,
            aktiveOverføringer = aktiveOverføringer,
            onDeaktivert = onDeaktivert,
            onNyTilOgMed = onNyTilOgMed
        )
    }

    private fun LocalDate.fraOgMedTilNyTilOgMed() = minusDays(1)

    private fun TransactionalSession.hentAktiveOverføringer(
        tabell: String, fra: Saksnummer, til: Saksnummer, tilOgMedEtter: LocalDate) : List<AktivOverføring> {
        val query = hentAktiveOverføringerQuery(tabell = tabell, fra = fra, til = til, tilOgMedEtter = tilOgMedEtter)
        return run(query.map { row -> AktivOverføring(
            id = row.long("id"),
            periode = Periode(
                fom = row.localDate("fom"),
                tom = row.localDate("tom")
            )
        )}.asList)
    }

    private fun TransactionalSession.deaktivertOverføringer(
        tabell: String, overføringer: List<AktivOverføring>) {
        val query = deaktivertOverføringQuery(
            tabell = tabell,
            overføringIder = overføringIderArray(overføringer)
        )
        run(query.asUpdate).also { oppdaterteRader -> if (oppdaterteRader != overføringer.size) {
            overføringer.map { it.id }.also {
                logger.warn("Forventet at overføringene $it skulle få status=Deaktivert, men $oppdaterteRader overføringer ble oppdatert.")
            }
        }}
    }

    private fun TransactionalSession.oppdaterTilOgMed(
        tabell: String, overføringer: List<AktivOverføring>, nyTilOgMed: LocalDate) {
        val query = endreTilOgMedQuery(
            tabell = tabell,
            overføringIder = overføringIderArray(overføringer),
            nyTilOgMed = nyTilOgMed
        )
        run(query.asUpdate).also { oppdaterteRader -> if (oppdaterteRader != overføringer.size) {
            overføringer.map { it.id }.also {
                logger.warn("Forventet at overføringene $it skulle få tom=$nyTilOgMed, men $oppdaterteRader overføringer ble oppdatert.")
            }
        }}
    }

    private fun Session.overføringIderArray(overføringer: Collection<AktivOverføring>) = createArrayOf("bigint", overføringer.map { it.id })

    private fun hentAktiveOveføringerStatement(tabell: String) =
        "SELECT id, fom, tom from $tabell WHERE fra = :fra AND til = :til AND tom > :tilOgMedEtter"
    private fun hentAktiveOverføringerQuery(tabell: String, fra: Saksnummer, til: Saksnummer, tilOgMedEtter: LocalDate) =
        queryOf(hentAktiveOveføringerStatement(tabell), mapOf(
            "fra" to fra, "til" to til, "tilOgMedEtter" to tilOgMedEtter
        ))

    private fun endreTilOgMedStatement(tabell: String) =
        "UPDATE $tabell SET tom = :nyTilOgMed WHERE id = ANY(:overforingIder)"
    private fun endreTilOgMedQuery(tabell: String, overføringIder: Array, nyTilOgMed: LocalDate) =
        queryOf(endreTilOgMedStatement(tabell), mapOf(
            "overforingIder" to overføringIder,
            "nyTilOgMed" to nyTilOgMed
        ))

    private fun deaktiverOverføringerStatement(tabell: String) =
        "UPDATE $tabell SET status = 'Deaktivert' WHERE id = ANY(:overforingIder)"
    private fun deaktivertOverføringQuery(tabell: String, overføringIder: Array) =
        queryOf(deaktiverOverføringerStatement(tabell), mapOf(
            "overforingIder" to overføringIder
        ))

    private val logger = LoggerFactory.getLogger(OpphørOverføringer::class.java)
}