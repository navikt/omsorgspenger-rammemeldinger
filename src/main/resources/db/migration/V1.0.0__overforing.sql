CREATE TABLE overforing
(
    id                          BIGSERIAL PRIMARY KEY,
    gjennomfort                 TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    fom                         DATE NOT NULL,
    tom                         DATE NOT NULL,
    fra                         VARCHAR(50) NOT NULL,
    til                         VARCHAR(50) NOT NULL,
    antall_dager                SMALLINT NOT NULL,
    status                      VARCHAR(100) NOT NULL,
    lovanvendelser              JSONB NOT NULL
);

CREATE INDEX index_overforing_fra ON overforing(fra);
CREATE INDEX index_overforing_til ON overforing(til);