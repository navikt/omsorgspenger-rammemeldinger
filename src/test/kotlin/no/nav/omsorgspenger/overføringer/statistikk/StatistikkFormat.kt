package no.nav.omsorgspenger.overføringer.statistikk

import no.nav.omsorgspenger.behovssekvens.BehovssekvensId
import no.nav.omsorgspenger.statistikk.StatistikkMelding
import no.nav.omsorgspenger.statistikk.jsonAssertEquals

internal object StatistikkFormat {

    private fun ForventetGjennomført(behovssekvensId: BehovssekvensId) = """
    {
        "saksnummer": "foo",
        "behandlingId": "$behovssekvensId",
        "mottattDato": "2021-01-01",
        "registrertDato": "2021-01-02",
        "behandlingType": "søknad",
        "behandlingStatus": "gjennomført",
        "funksjonellTid": "2021-01-18T17:10:00.654321+01:00",
        "tekniskTid": "2021-01-18T17:00:00.123456+01:00",
        "aktorId": "33",
        "ansvarligEnhetKode": "4487",
        "ansvarligEnhetType": "NORG",
        "versjon": 2,
        "avsender": "omsorgspenger-rammemeldinger",
        "totrinnsbehandling": false,
        "ytelseType": "omsorgspenger",
        "underType": "overføring"
    }
    """

    private fun ForventetAvslagSkjermet(behovssekvensId: BehovssekvensId) = """
    {
        "saksnummer": "foo",
        "behandlingId": "$behovssekvensId",
        "mottattDato": "2021-01-01",
        "registrertDato": "2021-01-02",
        "behandlingType": "søknad",
        "behandlingStatus": "avslått",
        "funksjonellTid": "2021-01-18T17:10:00.654321+01:00",
        "tekniskTid": "2021-01-18T17:00:00.123456+01:00",
        "aktorId": "-5",
        "ansvarligEnhetKode": "2103",
        "ansvarligEnhetType": "NORG",
        "versjon": 2,
        "avsender": "omsorgspenger-rammemeldinger",
        "totrinnsbehandling": false,
        "ytelseType": "omsorgspenger",
        "underType": "overføring"
    }
    """

    private fun forventetAvslag(behovssekvensId: BehovssekvensId) = """
    {
        "saksnummer": "foo",
        "behandlingId": "$behovssekvensId",
        "mottattDato": "2021-01-01",
        "registrertDato": "2021-01-02",
        "behandlingType": "søknad",
        "behandlingStatus": "avslått",
        "funksjonellTid": "2021-01-18T17:10:00.654321+01:00",
        "tekniskTid": "2021-01-18T17:00:00.123456+01:00",
        "aktorId": "33",
        "ansvarligEnhetKode": "4487",
        "ansvarligEnhetType": "NORG",
        "versjon": 2,
        "avsender": "omsorgspenger-rammemeldinger",
        "totrinnsbehandling": false,
        "ytelseType": "omsorgspenger",
        "underType": "overføring"
    }
    """

    internal fun StatistikkMelding.assertForventetGjennomført(behovssekvensId: BehovssekvensId) =
        jsonAssertEquals(ForventetGjennomført(behovssekvensId))

    internal fun StatistikkMelding.assertForventetAvslagSkjermet(behovssekvensId: BehovssekvensId) =
        jsonAssertEquals(ForventetAvslagSkjermet(behovssekvensId))

    internal fun StatistikkMelding.assertForventetAvslag(behovssekvensId: BehovssekvensId) =
        jsonAssertEquals(forventetAvslag(behovssekvensId))
}