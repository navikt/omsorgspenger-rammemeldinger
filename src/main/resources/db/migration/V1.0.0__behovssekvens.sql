CREATE TABLE behovssekvens
(
    id                  VARCHAR(100) NOT NULL PRIMARY KEY,
    status              VARCHAR(100) NOT NULL,
    behovssekvens       JSONB NOT NULL,
    sist_endret         TIMESTAMP WITH TIME ZONE NOT NULL
);