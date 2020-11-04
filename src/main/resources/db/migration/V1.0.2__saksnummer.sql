CREATE TABLE saksnummer
(
    id                          BIGSERIAL PRIMARY KEY,
    saksnummer                  VARCHAR(50) NOT NULL,
    identitetsnummer            VARCHAR(50) NOT NULL,
    UNIQUE (saksnummer, identitetsnummer)
);

create index INDEX_SAKSNUMMER_IDENTITETSNUMMER on saksnummer(identitetsnummer);
