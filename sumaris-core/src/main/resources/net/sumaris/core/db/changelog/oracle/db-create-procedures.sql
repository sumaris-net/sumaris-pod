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
--  16/05/19 BL Creation (add F_TO_RECTANGLE)
--


create or replace FUNCTION F_TO_RECTANGLE
(
  LAT IN number,
  LON IN number
)
RETURN VARCHAR2
AS
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
--$  example : select SIH2_ADAGIO_DBA.F_TO_RECTANGLE(47.6, -5.05) from DUAL; -- 24E4
--$            select SIH2_ADAGIO_DBA.F_TO_RECTANGLE(42.27, 5.4) from DUAL; -- M24C2
--$
--$ History :
--$  16/05/19 BL Creation (used by extraction - e.g. ICES RDB and COST formats)
--$
--$ ********************************************************************
  nbdemidegreeLat INTEGER;
  nbdemidegreeLong INTEGER;
  rest INTEGER;
  letter CHAR(1);
BEGIN
    IF (lat IS NULL OR lon IS NULL) THEN
        RETURN NULL;
    END IF;

    -- If position inside "Mediterranean and black sea" :
    IF  (((lon >= 0 AND lon < 42) AND (lat >= 30 AND lat < 47.5))
        OR ((lon >= -6 AND lon < 0) AND (lat >= 35 AND lat < 40))) THEN

        -- Number of rectangles, between the given latitude and 30°N :
        nbdemidegreeLat := FLOOR(lat-30) * 2;

        -- Number of rectangles, between the given longitude and 6°W :
        nbdemidegreeLong := FLOOR(lon+6) * 2;

        -- Letter change every 10 rectangles, starting with 'A' :
        letter := chr(FLOOR(nbdemidegreeLong / 10) + 65);
        rest := MOD(nbdemidegreeLong, 10);
        RETURN CONCAT(CONCAT(CONCAT('M', nbdemidegreeLat), letter), rest);

    -- If position inside "Atlantic (nord-east)" :
    ELSIF ((lon >= -50 AND lon <= 70) AND (lat >= 36 AND lat <= 89)) THEN
            nbdemidegreeLat := FLOOR((lat - 36) * 2) + 1;
        nbdemidegreeLong := FLOOR(lon + 50);
        letter := chr(FLOOR(nbdemidegreeLong / 10) + 65);
        rest := MOD(nbdemidegreeLong, 10);
        RETURN CONCAT(CONCAT(nbdemidegreeLat, letter), rest);
    END IF;

    RETURN NULL;
END;
//