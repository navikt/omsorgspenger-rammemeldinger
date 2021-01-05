CREATE TABLE koronaoverforing
(
    id                              BIGSERIAL PRIMARY KEY,
    gjennomfort                     TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    fom                             DATE NOT NULL,
    tom                             DATE NOT NULL,
    fra                             VARCHAR(50) NOT NULL,
    til                             VARCHAR(50) NOT NULL,
    antall_dager                    SMALLINT NOT NULL,
    antall_dager_onsket_overfort    SMALLINT NOT NULL,
    status                          VARCHAR(100) NOT NULL DEFAULT 'Aktiv',
    lovanvendelser                  JSONB NOT NULL,
    behovssekvens_id                VARCHAR(100) NOT NULL
);

CREATE INDEX index_koronaoverforing_fra ON koronaoverforing(fra);
CREATE INDEX index_koronaoverforing_til ON koronaoverforing(til);