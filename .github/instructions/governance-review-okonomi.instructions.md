---
applyTo: "**"
excludeAgent: "cloud-agent"
---

# Økonomireglementet — spesielle hensyn

Kodebasen forvalter ytelser/utbetalinger underlagt økonomireglementet. Flagg for menneskelig reviewer.

## Uhensiktsmessig spesialbehandling
Logikk som behandler enkeltpersoner/organisasjoner/saker ulikt uten saklig grunnlag: hardkodede aktørId, FNR, saksnummer, orgnummer i forretningslogikk. Unntak: testdata, midlertidige filtre med kommentar + oppfølgingssak.

## Flyway-migrasjoner (økonomisk data)
Tabeller med finansielle data (ytelse, beregning, oppdrag, tilbakekreving, vedtak, utbetaling, refusjon). Destruktive migrasjoner (DROP TABLE/COLUMN, ALTER type). Feil versjonsnummerering. Endring av beregningsgrunnlag/satser/vedtaksdata.

## Sporbarhet
PR-beskrivelse skal inneholde lenke (Jira, issue, Slack) eller tydelig **hvorfor**. Forretningslogikk uten sporbar begrunnelse → flagg.

## Rimelighet
Rimelig omfang vs. beskrevet behov. Utilsiktede sideeffekter på beregning/utbetaling. Feilhåndteringsendringer som kan føre til feilutbetalinger/tapte krav.
