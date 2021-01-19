package no.nav.omsorgspenger.statistikk

import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate
import java.time.OffsetDateTime

private const val overstyrtMotattDato = "2021-01-01"
private const val overstyrtRegistrertDato = "2021-01-02"
private const val overstyrtTekniskTid = "2021-01-18T17:00:00.123456+01:00"
private const val overstyrtFunksjonelTid = "2021-01-18T17:10:00.654321+01:00"

private fun StatistikkMelding.overstyrTidspunkt() = copy(
    mottattDato = LocalDate.parse(overstyrtMotattDato),
    registrertDato = LocalDate.parse(overstyrtRegistrertDato),
    tekniskTid = OffsetDateTime.parse(overstyrtTekniskTid),
    funksjonellTid = OffsetDateTime.parse(overstyrtFunksjonelTid)
)

internal fun StatistikkMelding.jsonAssertEquals(expected: String) =
    JSONAssert.assertEquals(expected, this.overstyrTidspunkt().toJson(), true)
