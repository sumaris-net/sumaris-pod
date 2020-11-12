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

DROP FUNCTION F_TO_RECTANGLE IF EXISTS;
//

-- Convert lat/lon into ICES or CGPM rectangle
-- See doc: Locations.getRectangleLabelByLatLong()
CREATE FUNCTION F_TO_RECTANGLE(lat DOUBLE, lon DOUBLE)
    RETURNS VARCHAR(5)
BEGIN ATOMIC
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
    DECLARE nbdemidegreeLat, nbdemidegreeLong, rest INTEGER;
    DECLARE letter CHAR(1);

    IF (lat IS NULL OR lon IS NULL) THEN
        RETURN NULL;
    END IF;

    -- If position inside "Mediterranean and black sea" :
    IF  (((lon >= 0 AND lon < 42) AND (lat >= 30 AND lat < 47.5))
        OR ((lon >= -6 AND lon < 0) AND (lat >= 35 AND lat < 40))) THEN

        -- Number of rectangles, between the given latitude and 30°N :
        SET nbdemidegreeLat = FLOOR(lat-30) * 2;

        -- Number of rectangles, between the given longitude and 6°W :
        SET nbdemidegreeLong = FLOOR(lon+6) * 2;

        -- Letter change every 10 rectangles, starting with 'A' :
        SET letter = CHAR(FLOOR(nbdemidegreeLong / 10) + 65);
        SET rest = MOD(nbdemidegreeLong, 10);
        RETURN CONCAT('M', nbdemidegreeLat, letter, rest);

        -- If position inside "Atlantic (nord-east)" :
    ELSEIF ((lon >= -50 AND lon <= 70) AND (lat >= 36 AND lat <= 89)) THEN
        SET nbdemidegreeLat = FLOOR((lat - 36) * 2) + 1;
        SET nbdemidegreeLong = FLOOR(lon + 50);
        SET letter = CHAR(FLOOR(nbdemidegreeLong / 10) + 65);
        SET rest = MOD(nbdemidegreeLong, 10);
        RETURN CONCAT(nbdemidegreeLat, letter, rest);
    END IF;

    RETURN NULL;
END;
//

DROP FUNCTION F_TO_SQUARE IF EXISTS;
//

-- Convert lat/lon into a square label
-- See doc: Locations.getSquare10LabelByLatLong()
CREATE FUNCTION F_TO_SQUARE(lat DOUBLE, lon DOUBLE, square_size INT)
    RETURNS VARCHAR(10)
BEGIN ATOMIC
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
    DECLARE quadrant INTEGER;
    DECLARE absLat, absLon DOUBLE;
    DECLARE intLatitude, decLatitude, intLongitude, decLongitude INTEGER;
    DECLARE resultLatitude, resultLongitude VARCHAR(5);

    IF (lat IS NULL OR lon IS NULL) THEN
        RETURN NULL;
    END IF;

    -- Get the quadrant
    IF (lon <= 0 AND lat >= 0) THEN
        SET quadrant = 1;
    ELSEIF (lon > 0 AND lat > 0) THEN
        SET quadrant = 2;
    ELSEIF (lon > 0 AND lat < 0) THEN
        SET quadrant = 3;
    ELSE
        SET quadrant = 4;
    END IF;

    -- Latitude
    SET absLat = ABS(lat);
    SET intLatitude = FLOOR(absLat);
    SET decLatitude = FLOOR((absLat - intLatitude) * 60 / square_size);
    --SET resultLatitude = LPAD(intLatitude, 2, '0');
    IF(square_size >= 10) THEN
        SET resultLatitude = CONCAT(LPAD(intLatitude, 2, '0'), decLatitude);
    ELSE
        SET resultLatitude = CONCAT(LPAD(intLatitude, 2, '0'), LPAD(decLatitude, 2, '0'));
    END IF;

    -- Longitude
    SET absLon = ABS(lon);
    SET intLongitude = FLOOR(absLon);
    SET decLongitude = FLOOR((absLon - intLongitude) * 60 / square_size);
    IF(square_size >= 10) THEN
        SET resultLongitude = CONCAT(LPAD(intLongitude, 3, '0'), decLongitude);
    ELSE
        SET resultLongitude = CONCAT(LPAD(intLongitude, 3, '0'), LPAD(decLongitude, 2, '0'));
    END IF;

    RETURN CONCAT(quadrant, resultLatitude, resultLongitude);
END;
//

DROP FUNCTION F_HASH_CODE IF EXISTS;
//

CREATE FUNCTION F_HASH_CODE(EXPR VARCHAR(100))
    RETURNS INTEGER
BEGIN ATOMIC
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
    DECLARE LEN INTEGER;
    DECLARE VAR INTEGER;
    DECLARE HASH BIGINT default 0;
    DECLARE COUNTER INTEGER default 0;
    DECLARE INTEGER_MAX BIGINT default 2147483647;
    DECLARE INTEGER_MIN BIGINT default -2147483648;

    IF (EXPR IS NULL) THEN
        return NULL;
    END IF;

    SET LEN = CHAR_LENGTH(EXPR);
    IF (LEN = 0) THEN
        RETURN 0;
    END IF;

    WHILE (COUNTER < LEN) DO
        SET VAR = ASCII(SUBSTRING(EXPR, COUNTER+1, 1));
        SET HASH = HASH * 31 + VAR;

        -- java.lang.Integer.MAX_VALUE = 2 147 483 647
        WHILE (HASH > INTEGER_MAX) DO
            SET HASH = HASH + INTEGER_MIN * 2;
        END WHILE;
        WHILE (HASH < INTEGER_MIN) DO
            SET HASH = HASH - INTEGER_MIN * 2;
        END WHILE;

        SET COUNTER = COUNTER + 1;
    END WHILE;

    RETURN CAST(HASH AS INTEGER);
END;
//