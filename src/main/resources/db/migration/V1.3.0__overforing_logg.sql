CREATE TABLE overforing_logg
(
    id                          BIGSERIAL PRIMARY KEY,
    overforing_id               BIGINT NOT NULL,
    behovssekvens_id            VARCHAR(100) NOT NULL,
    tidspunkt                   TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    melding                     VARCHAR(280) NOT NULL,
    CONSTRAINT foreign_key_overforing FOREIGN KEY(overforing_id) REFERENCES overforing(id)
);
