ALTER TABLE overforing ADD COLUMN antall_dager_onsket_overfort SMALLINT NOT NULL DEFAULT 0;

-- Fire som allerede er gjort i produksjon
-- 01EQNZ85BXJ5GBA5AGCDG3MSWY
UPDATE overforing SET antall_dager_onsket_overfort = 10 WHERE fra = '5yc1s' AND til = '5yc36';
-- 01EQT5G8D0DZAGR636E6Q099YK
UPDATE overforing SET antall_dager_onsket_overfort = 7 WHERE fra = '5yc4k' AND til = '5yc5y';
-- 01EQVD9TF8DF4KNPCCJCA0Y331
UPDATE overforing SET antall_dager_onsket_overfort = 10 WHERE fra = '5yc7c' AND til = '5yc8q';
-- 01EQVESZMQ3MXF5X2JK5A5Q6HV
UPDATE overforing SET antall_dager_onsket_overfort = 5 WHERE fra = '5yca4' AND til = '5ycbi';

