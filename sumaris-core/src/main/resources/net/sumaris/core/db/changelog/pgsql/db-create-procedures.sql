---
-- #%L
-- SUMARiS:: Core
-- %%
-- Copyright (C) 2018 - 2019 SUMARiS Consortium
-- %%
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as
-- published by the Free Software Foundation, either version 3 of the
-- License, or (at your option) any later version.
-- 
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
-- 
-- You should have received a copy of the GNU General Public
-- License along with this program.  If not, see
-- <http://www.gnu.org/licenses/gpl-3.0.html>.
-- #L%
---
-- --------------------------------------------------------------------------
--
-- Execute PL/SQL and create procedures.
-- The following functions/procedures are created:
--  - function F_RECTANGLE to compute rectangle label, from lat/lon
--
--  project : ${pom.name}
--  version : ${pom.version} for ${pom.env}
--      env : ${pom.env} - ${pom.profil}
--     date : ${pom.date.fr}
--
--  License: AGPL v3 License
--
--  history :
--  16/05/19 BL Creation (add F_TO_RECTANGLE and F_TO_SQUARE)
--

DROP FUNCTION IF EXISTS F_TO_RECTANGLE;
//

-- Convert lat/lon into ICES or CGPM rectangle
-- See doc: Locations.getRectangleLabelByLatLong()
CREATE FUNCTION F_TO_RECTANGLE(lat DECIMAL, lon DECIMAL)
    RETURNS VARCHAR(5)
    LANGUAGE plpgsql
AS $$

--$ ********************************************************************
--$
--$  MOD : F_TO_RECTANGLE
--$  ROL : Compute the statistical rectangle (ICES or CGPM)
--$  param :
--$    - LAT: latitude, in decimal degrees
--$    - LON: longitude, in decimal degrees
--$
--$  return : the rectangle label
--$
--$  example : select SIH2_ADAGIO_DBA.F_TO_RECTANGLE(47.6, -5.05) from STATUS where ID=1; -- 24E4
--$            select SIH2_ADAGIO_DBA.F_TO_RECTANGLE(42.27, 5.4) from STATUS where ID=1; -- M24C2
--$
--$ History :
--$  16/05/19 BL Creation (used by extraction - e.g. ICES RDB and COST formats)
--$
--$ ********************************************************************
DECLARE
    nbdemidegreeLat INTEGER;
    nbdemidegreeLong INTEGER;
    rest INTEGER;
    letter CHAR(1);
BEGIN
    IF lat IS NULL OR lon IS NULL THEN
        RETURN NULL;
    END IF;

    -- If position inside "Mediterranean and black sea" :
    if  ((lon >= 0 AND lon < 42) AND (lat >= 30 AND lat < 47.5))
        or ((lon >= -6 AND lon < 0) AND (lat >= 35 AND lat < 40)) then

        -- Number of rectangles, between the given latitude and 30°N :
        nbdemidegreeLat := floor(lat-30) * 2;

        -- Number of rectangles, between the given longitude and 6°W :
        nbdemidegreeLong := FLOOR(lon+6) * 2;

        -- Letter change every 10 rectangles, starting with 'A' :
        letter := CHR(CAST(FLOOR(nbdemidegreeLong / 10) + 65 AS INTEGER));
        rest := MOD(nbdemidegreeLong, 10);
        RETURN CONCAT('M', nbdemidegreeLat, letter, rest);

        -- If position inside "Atlantic (nord-east)" :
    ELSEIF ((lon >= -50 AND lon <= 70) AND (lat >= 36 AND lat <= 89)) THEN
        nbdemidegreeLat := FLOOR((lat - 36) * 2) + 1;
        nbdemidegreeLong := FLOOR(lon + 50);
        letter := CHR(CAST(FLOOR(nbdemidegreeLong / 10) + 65 AS INTEGER));
        rest := MOD(nbdemidegreeLong, 10);
        RETURN CONCAT(nbdemidegreeLat, letter, rest);
    END IF;

    RETURN NULL;
END;
$$
//

DROP FUNCTION IF EXISTS F_TO_SQUARE ;
//

-- Convert lat/lon into a square label
-- See doc: Locations.getSquare10LabelByLatLong()
CREATE FUNCTION F_TO_SQUARE(lat DECIMAL, lon DECIMAL, square_size INT)
    RETURNS VARCHAR(10)
    LANGUAGE plpgsql
AS $$
--$ ********************************************************************
--$
--$  MOD : F_TO_SQUARE
--$  ROL : Compute the square code
--$  param :
--$    - lat: latitude, in decimal degrees
--$    - lon: longitude, in decimal degrees
--$    - square_size: size of the square, in minutes
--$
--$  return : the square code
--$
--$ History :
--$  16/05/19 BL Creation (used by aggregation)
--$
--$ ********************************************************************
DECLARE
    quadrant INTEGER;
    absLat DECIMAL;
    absLon DECIMAL;
    intLatitude INTEGER;
    decLatitude INTEGER;
    intLongitude INTEGER;
    decLongitude INTEGER;
    resultLatitude VARCHAR(5);
    resultLongitude VARCHAR(5);
BEGIN

    IF (lat IS NULL OR lon IS NULL) THEN
        RETURN NULL;
    END IF;

    -- Get the quadrant
    IF (lon <= 0 AND lat >= 0) THEN
        quadrant := 1;
    ELSEIF (lon > 0 AND lat > 0) THEN
        quadrant := 2;
    ELSEIF (lon > 0 AND lat < 0) THEN
        quadrant := 3;
    ELSE
        quadrant := 4;
    END IF;

    -- Latitude
    absLat := ABS(lat);
    intLatitude := FLOOR(absLat);
    decLatitude := FLOOR((absLat - intLatitude) * 60 / square_size);
    --SET resultLatitude = LPAD(intLatitude, 2, '0');
    IF(square_size >= 10) THEN
        resultLatitude := CONCAT(LPAD(intLatitude::VARCHAR, 2, '0'), decLatitude);
    ELSE
        resultLatitude := CONCAT(LPAD(intLatitude::VARCHAR, 2, '0'), LPAD(decLatitude::VARCHAR, 2, '0'));
    END IF;

    -- Longitude
    absLon := ABS(lon);
    intLongitude := FLOOR(absLon);
    decLongitude := FLOOR((absLon - intLongitude) * 60 / square_size);
    IF(square_size >= 10) THEN
        resultLongitude := CONCAT(LPAD(intLongitude::VARCHAR, 3, '0'), decLongitude);
    ELSE
        resultLongitude := CONCAT(LPAD(intLongitude::VARCHAR, 3, '0'), LPAD(decLongitude::VARCHAR, 2, '0'));
    END IF;

    RETURN CONCAT(quadrant, resultLatitude, resultLongitude);
END;
$$
//

DROP FUNCTION IF EXISTS F_HASH_CODE;
//

CREATE FUNCTION F_HASH_CODE(EXPR VARCHAR(100))
    RETURNS INTEGER
    LANGUAGE plpgsql
AS $$
--$ ********************************************************************
--$
--$  MOD : F_HASH_CODE
--$  ROL : Convert a string to a hash code (return a number)
--$  param :
--$    - EXPR: the expression to convert
--$
--$  return : a number, that represent the 'EXPR' string (or NULL, is EXPR is null)
--$
--$  example : select SIH2_ADAGIO_DBA.F_HASH('value1') from STATUS where ID=1; --
--$            call SIH2_ADAGIO_DBA.F_HASH('value1'); --
--$
--$ History :
--$  28/10/20 BL Creation (used by aggregation, over a RDB/COST extractions)
--$
--$ ********************************************************************
DECLARE
    LEN INTEGER;
    VAR INTEGER;
    HASH BIGINT default 0;
    COUNTER INTEGER default 0;
    INTEGER_MAX BIGINT default 2147483647;
    INTEGER_MIN BIGINT default -2147483648;
BEGIN
    IF (EXPR IS NULL) THEN
        return NULL;
    END IF;

    LEN := CHAR_LENGTH(EXPR);
    IF (LEN = 0) THEN
        RETURN 0;
    END IF;

    WHILE COUNTER < LEN LOOP
        VAR := ASCII(SUBSTRING(EXPR, COUNTER+1, 1));
        HASH := HASH * 31 + VAR;

        -- java.lang.Integer.MAX_VALUE = 2 147 483 647
        WHILE (HASH > INTEGER_MAX) LOOP
            HASH := HASH + INTEGER_MIN * 2;
        END LOOP;
        WHILE (HASH < INTEGER_MIN) LOOP
            HASH := HASH - INTEGER_MIN * 2;
        END LOOP;

        COUNTER := COUNTER + 1;
    END LOOP;

    RETURN CAST(HASH AS INTEGER);
END;
$$
//

DROP FUNCTION IF EXISTS F_DATE_EXTRACT;
//

DROP FUNCTION IF EXISTS F_DATEDIFF_MINUTE;
//
DROP FUNCTION IF EXISTS DATEDIFF;
//
DROP FUNCTION IF EXISTS F_DATEDIFF;
//
CREATE FUNCTION F_DATEDIFF (units VARCHAR(30), start_t TIMESTAMP, end_t TIMESTAMP)
     RETURNS INT AS $$
DECLARE
    diff_interval INTERVAL;
    diff INT = 0;
    years_diff INT = 0;
BEGIN
     IF units IN ('yy', 'yyyy', 'year', 'mm', 'm', 'month') THEN
       years_diff = DATE_PART('year', end_t) - DATE_PART('year', start_t);

        IF units IN ('yy', 'yyyy', 'year') THEN
                 -- SQL Server does not count full years passed (only difference between year parts)
                 RETURN years_diff;
        ELSE
                 -- If end month is less than start month it will subtracted
                 RETURN years_diff * 12 + (DATE_PART('month', end_t) - DATE_PART('month', start_t));
        END IF;
    END IF;

     -- Minus operator returns interval 'DDD days HH:MI:SS'
    diff_interval = end_t - start_t;

    diff = diff + DATE_PART('day', diff_interval);

    IF units IN ('wk', 'ww', 'week') THEN
        diff = diff/7;
        RETURN diff;
    END IF;

    IF units IN ('dd', 'd', 'day') THEN
           RETURN diff;
    END IF;

    diff = diff * 24 + DATE_PART('hour', diff_interval);

    IF units IN ('hh', 'hour') THEN
            RETURN diff;
    END IF;

    diff = diff * 60 + DATE_PART('minute', diff_interval);

    IF units IN ('mi', 'n', 'minute') THEN
            RETURN diff;
    END IF;

    diff = diff * 60 + DATE_PART('second', diff_interval);

    RETURN diff;
END;
$$ LANGUAGE plpgsql;
//