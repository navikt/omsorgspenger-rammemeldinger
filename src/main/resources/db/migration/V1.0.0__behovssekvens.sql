CREATE TABLE BEHOVSSEKVENS
(
    id                  VARCHAR(100) NOT NULL PRIMARY KEY,
    status              VARCHAR(100) NOT NULL,
    behovssekvens       jsonb NOT NULL,
    sist_endret         TIMESTAMP WITH TIME ZONE NOT NULL
);