CREATE TABLE behovssekvens
(
    id                  BIGSERIAL PRIMARY KEY,
    behovssekvens_id    VARCHAR(100) NOT NULL,
    behovssekvens       JSONB NOT NULL,
    gjennomfort         TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    gjennomfort_steg    VARCHAR(100) NOT NULL
);