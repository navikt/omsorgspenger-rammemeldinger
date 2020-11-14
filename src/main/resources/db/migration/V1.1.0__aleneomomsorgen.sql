CREATE TABLE aleneomomsorgen
(
    id                          BIGSERIAL PRIMARY KEY,
    registrert                  TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    registrert_ifbm             VARCHAR(100) NOT NULL,
    saksnummer                  VARCHAR(100) NOT NULL,
    fom                         DATE NOT NULL,
    tom                         DATE NOT NULL,
    barn_identitetsnummer       VARCHAR(25) NOT NULL,
    barn_fodselsdato            DATE NOT NULL,
    behovssekvens_id            VARCHAR(100) NOT NULL,
    status                      VARCHAR(100) NOT NULL default 'Aktiv',
    UNIQUE(saksnummer, barn_identitetsnummer)
);

CREATE INDEX index_aleneomomsorgen_saksnummer ON aleneomomsorgen(saksnummer);