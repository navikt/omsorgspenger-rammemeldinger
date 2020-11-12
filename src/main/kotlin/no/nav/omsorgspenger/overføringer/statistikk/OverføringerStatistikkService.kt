package no.nav.omsorgspenger.overføringer.statistikk

import no.nav.k9.statistikk.kontrakter.Behandling
import no.nav.k9.statistikk.kontrakter.Sak
import org.apache.kafka.clients.producer.KafkaProducer
import java.time.OffsetDateTime

internal class OverføringerStatistikkService(
        private val kafkaProducer: KafkaProducer<String, String>,
        private val topic: String = "privat-k9-TODO-???") { // TODO: TOPIC for sak og behandling

    fun publiser(statistikk: OverføringStatistikkMelding) {
        val tekniskTid = OffsetDateTime.now()

        val saksnummer = statistikk.saksnummer

        val avsender = "omsorgspenger-rammemeldinger"

        val sak = Sak(
                /*  Tidspunktet da hendelsen faktisk ble gjennomført eller registrert i kildesystemet. Dette er det
                    tidspunkt der hendelsen faktisk er gjeldende fra. Ved for eksempel patching av data eller
                    oppdatering tilbake i tid, skal tekniskTid være lik endringstidspunktet, mens funksjonellTid angir
                    tidspunktet da endringen offisielt gjelder fra. */
                funksjonellTid = null, // todo

                tekniskTid = tekniskTid,
                opprettetDato = null, // todo: datoen hvor saksnumret blir opprettet
                sakId = saksnummer,

                /*  Aktør IDen til primær mottager av ytelsen om denne blir godkjent. Altså, den som saken omhandler. */
                aktorId = null, // todo

                saksnummer = saksnummer,
                ytelseType = null, // todo

                /*  Kode som angir sakens status, slik som påbegynt, under utbetaling, avsluttet o.l. */
                sakStatus = null, // todo

                avsender = avsender,
                versjon = 1
        )

        val behandling = Behandling(
                behandlingId = statistikk.behandlingId,

                /*  Tidspunktet da hendelsen faktisk ble gjennomført eller registrert i kildesystemet. Dette er det
                    tidspunkt der hendelsen faktisk er gjeldende fra. Ved for eksempel patching av data eller
                    oppdatering tilbake i tid, skal tekniskTid være lik endringstidspunktet, mens funksjonellTid angir
                    tidspunktet da endringen offisielt gjelder fra. */
                funksjonellTid = null, // todo

                tekniskTid = tekniskTid,

                /*  Denne datoen forteller fra hvilken dato behandlingen først ble initiert.
                    Datoen brukes i beregning av saksbehandlingstid og skal samsvare med brukerens opplevelse av at
                    saksbehandlingen har startet. */
                mottattDato = statistikk.mottaksdato,

                /*  Tidspunkt for når behandlingen ble registrert i saksbehandlingssystemet. Denne kan avvike fra
                    mottattDato hvis det tar tid fra postmottak til registrering i system, eller hvis en oppgave om å
                    opprette behandling ligger på vent et sted i NAV. Ved automatisk registrering av saker er denne
                    samme som mottattDato. */
                registrertDato = null, // todo

                sakId = saksnummer,
                saksnummer = saksnummer,
                behandlingType = null, // todo
                behandlingStatus = null, // todo

                /*  Kode som beskriver behandlingens  utlandstilsnitt i henhold til NAV spesialisering. I hoved sak vil
                    denne koden beskrive om saksbehandlingsfrister er i henhold til utlandssaker eller innlandssaker,
                    men vil for mange kildesystem være angitt med en høyere oppløsning. */
                utenlandstilsnitt = null, // todo

                ansvarligEnhetKode = null, // todo, konstant?
                ansvarligEnhetType = null, // todo, konstant?
                behandlendeEnhetKode = null, // todo, konstant?
                behandlendeEnhetType = null, // todo, konstant?

                totrinnsbehandling = false,
                avsender = avsender,
                versjon = 1
        )
    }
}