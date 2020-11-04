CREATE TABLE saksnummer
(
    id                          BIGSERIAL PRIMARY KEY,
    sak                         VARCHAR(50) NOT NULL,
    identitetsnummer            VARCHAR(50) NOT NULL,
    UNIQUE (sak, identitetsnummer)
);

CREATE INDEX index_saksnummer_sak ON saksnummer(sak);
