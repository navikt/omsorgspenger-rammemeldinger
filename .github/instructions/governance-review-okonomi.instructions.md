---
applyTo: "**"
excludeAgent: "cloud-agent"
---

# Økonomireglementet — spesielle hensyn

Denne kodebasen forvalter ytelser og utbetalinger underlagt økonomireglementet (Reglement for økonomistyring i staten).
Copilot skal spesielt vurdere følgende:

## Uhensiktsmessig spesialbehandling
- Flagg kode som innfører logikk for å behandle enkeltpersoner, enkeltorganisasjoner eller spesifikke saker ulikt uten saklig grunnlag
- Eksempler: hardkodede aktørId-er, fødselsnumre, saksnumre eller organisasjonsnumre som styrer forretningslogikk
- Unntak: testdata, feiltoleransefiltre med tydelig midlertidig kommentar og tilhørende oppfølgingssak

## Flyway-migrasjoner (økonomisk data)
- Migrasjonsfiler som endrer tabeller med finansielle data (ytelse, beregning, oppdrag, tilbakekreving, vedtak, utbetaling, refusjon)
- Destruktive migrasjoner (DROP TABLE, DROP COLUMN, ALTER COLUMN type change) på produksjonstabeller
- Manglende eller feil versjonsnummerering i migrasjonsfiler
- Migrasjoner som endrer beregningsgrunnlag, satser eller vedtaksdata krever ekstra oppmerksomhet

## Sporbarhet og begrunnelse
- Sjekk at PR-beskrivelsen inneholder minst én av:
  - Lenke til Jira-sak, GitHub issue eller Slack-tråd
  - En tydelig beskrivelse av **hvorfor** endringen gjøres
- Endringer i forretningslogikk uten sporbar begrunnelse skal flagges

## Rimelighet og proporsjonalitet
- Vurder om endringene er rimelige i omfang relativt til beskrevet behov
- Flagg hvis endringen har utilsiktede sideeffekter på beregning eller utbetaling
- Varsle hvis feilhåndtering endres på måter som kan føre til feilutbetalinger eller tapte krav
