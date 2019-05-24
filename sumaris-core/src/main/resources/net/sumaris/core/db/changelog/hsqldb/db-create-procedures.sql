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