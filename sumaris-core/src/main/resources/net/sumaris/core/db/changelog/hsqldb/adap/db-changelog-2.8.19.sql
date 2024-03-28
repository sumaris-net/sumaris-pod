
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

-- Replace ALIVE with FRESH
update SORTING_MEASUREMENT_B set QUALITATIVE_VALUE_FK=367 where SORTING_MEASUREMENT_B.BATCH_FK IS NOT NULL AND QUALITATIVE_VALUE_FK=368;

-- Replace DRESSING 'GTA' (Eviscéré et équeuté) with 'GUT' (Eviscéré)
update SORTING_MEASUREMENT_B set QUALITATIVE_VALUE_FK=339 where SORTING_MEASUREMENT_B.BATCH_FK IS NOT NULL AND QUALITATIVE_VALUE_FK=338;

-- Create an index on LOCATION.LABEL + LOCATION_LEVEL
drop index LOCATION_LABEL_IDX if exists;
create index LOCATION_LABEL_IDX ON LOCATION(LABEL, LOCATION_LEVEL_FK);

drop table TMP_BATCH IF EXISTS ;
drop sequence TMP_BATCH_SEQ IF EXISTS ;
create sequence TMP_BATCH_SEQ AS INTEGER START WITH 1;
create table TMP_BATCH AS (
                              select
                                  NEXT VALUE FOR TMP_BATCH_SEQ as ID,
                                  T.ID as TRIP_FK,
                                  O.ID as OPERATION_FK,
                                  extract(year from O.END_DATE_TIME) as YEAR,
                                  extract(month from O.END_DATE_TIME) as MONTH,
                                  COALESCE(O.END_DATE_TIME, O.START_DATE_TIME) as DATE_TIME,
                                  VP.LATITUDE,
                                  VP.LONGITUDE,
                                  L_RECT.ID RECTANGLE_FK,
                                  L_RECT.LABEL RECTANGLE_LABEL,
                                  TG.ID as TAXON_GROUP_FK,
                                  TG.LABEL AS TAXON_GROUP_LABEL,
                                  SPECIES_QM.ID as TAXON_GROUP_WEIGHT_MEASUREMENT_ID,
                                  SPECIES_QM.PMFM_FK as TAXON_GROUP_WEIGHT_PMFM_FK,
                                  SPECIES_QM.NUMERICAL_VALUE as TAXON_GROUP_WEIGHT,
                                  SAMPLE_B.ID as SAMPLE_BATCH_FK,
                                  SAMPLE_B.SAMPLING_RATIO as SAMPLING_RATIO,
                                  SAMPLE_B.SAMPLING_RATIO_TEXT as SAMPLING_RATIO_TEXT,
                                  SAMPLE_QM.ID as SAMPLE_WEIGHT_MEASUREMENT_ID,
                                  SAMPLE_QM.PMFM_FK as SAMPLE_WEIGHT_PMFM_FK,
                                  SAMPLE_QM.NUMERICAL_VALUE as SAMPLE_WEIGHT,
                                  cast(null as DOUBLE) as NEW_SAMPLE_RTP_WEIGHT, -- Will be computed later
                                  LENGTH_B.ID as LENGTH_BATCH_FK,
                                  LENGTH_B.REFERENCE_TAXON_FK as REFERENCE_TAXON_FK,
                                  TN.LABEL as REFERENCE_TAXON_LABEL,
                                  LENGTH_QM.ID as RTP_MEASUREMENT_ID,
                                  LENGTH_QM.NUMERICAL_VALUE as RTP_WEIGHT,
                                  cast(null as DOUBLE) as NEW_RTP_WEIGHT, -- Will be computed later
                                  LENGTH_PMFM.ID as LENGTH_PMFM_FK,
                                  LENGTH_PMFM.PARAMETER_FK as LENGTH_PARAMETER_FK,
                                  LENGTH_PMFM.UNIT_FK as LENGTH_UNIT_FK,
                                  LENGTH_SM.NUMERICAL_VALUE as LENGTH_VALUE,
                                  DRESSING_QV.ID as DRESSING_FK,
                                  DRESSING_QV.LABEL as DRESSING_LABEL,
                                  PRESERVATION_QV.ID as PRESERVATION_FK,
                                  PRESERVATION_QV.LABEL as PRESERVATION_LABEL
                              from
                                  BATCH LENGTH_B
                                      inner join QUANTIFICATION_MEASUREMENT_B LENGTH_QM on LENGTH_QM.BATCH_FK=LENGTH_B.ID AND LENGTH_QM.PMFM_FK=122 and LENGTH_QM.IS_REFERENCE_QUANTIFICATION=true-- RTP WEIGHT
                                      inner join SORTING_MEASUREMENT_B LENGTH_SM on LENGTH_SM.BATCH_FK=LENGTH_B.ID AND LENGTH_SM.NUMERICAL_VALUE IS NOT NULL -- LENGTH_SM.PMFM_FK=81 -- LENGTH
                                      inner join PMFM LENGTH_PMFM on LENGTH_PMFM.ID=LENGTH_SM.PMFM_FK
                                      inner join OPERATION O on O.ID=LENGTH_B.OPERATION_FK
                                      inner join TRIP T on T.ID=O.TRIP_FK
                                      inner join VESSEL_POSITION VP ON VP.OPERATION_FK=O.ID and VP.DATE_TIME=O.END_DATE_TIME
                                      inner join BATCH SAMPLE_B on SAMPLE_B.ID=LENGTH_B.PARENT_BATCH_FK
                                      inner join BATCH LANDING_B on LANDING_B.ID=SAMPLE_B.PARENT_BATCH_FK
                                      inner join SORTING_MEASUREMENT_B LANDING_SM on LANDING_SM.BATCH_FK=LANDING_B.ID AND LANDING_SM.QUALITATIVE_VALUE_FK=190 -- LANDING
                                      inner join SORTING_MEASUREMENT_B DRESSING_B on DRESSING_B.BATCH_FK=LANDING_B.ID AND DRESSING_B.PMFM_FK=151 -- DRESSING
                                      inner join QUALITATIVE_VALUE DRESSING_QV ON DRESSING_QV.ID=DRESSING_B.QUALITATIVE_VALUE_FK
                                      inner join BATCH SPECIES_B on SPECIES_B.ID=LANDING_B.PARENT_BATCH_FK AND SPECIES_B.TAXON_GROUP_FK IS NOT NULL
                                      inner join TAXON_NAME TN ON TN.REFERENCE_TAXON_FK=LENGTH_B.REFERENCE_TAXON_FK AND TN.IS_REFERENT=true
                                      inner join TAXON_GROUP TG ON TG.ID=SPECIES_B.TAXON_GROUP_FK
                                      left outer join LOCATION L_RECT ON L_RECT.LABEL = F_TO_RECTANGLE(VP.LATITUDE, VP.LONGITUDE) AND L_RECT.LOCATION_LEVEL_FK in (4,5) /*rect stat*/
                                      left outer join QUANTIFICATION_MEASUREMENT_B SAMPLE_QM on SAMPLE_QM.BATCH_FK=SAMPLE_B.ID AND SAMPLE_QM.IS_REFERENCE_QUANTIFICATION=true --AND SAMPLE_QM.PMFM_FK in (91,92,93,123) -- SUM RTP WEIGHT
                                      left outer join QUANTIFICATION_MEASUREMENT_B SPECIES_QM on SPECIES_QM.BATCH_FK=SPECIES_B.ID AND SPECIES_QM.IS_REFERENCE_QUANTIFICATION=true -- AND SPECIES_QM.PMFM_FK in (91,92,93) -- Total weight
                                      left outer join SORTING_MEASUREMENT_B PRESERVATION_B on PRESERVATION_B.BATCH_FK=LANDING_B.ID AND PRESERVATION_B.PMFM_FK=150 -- PRESERVATION
                                      left outer join QUALITATIVE_VALUE PRESERVATION_QV ON PRESERVATION_QV.ID=PRESERVATION_B.QUALITATIVE_VALUE_FK
                              where LENGTH_B.LABEL LIKE 'SORTING_BATCH_INDIVIDUAL#%'
) WITH DATA;

create index TMP_BATCH_LENGTH_BATCH_FK ON TMP_BATCH(LENGTH_BATCH_FK);
--select count(*) from TMP_BATCH;

-- Analyse des poids de reference en doublon (RTP + standard)
/*select T1.* from TMP_BATCH T1
                     inner join TMP_BATCH T2 on T1.ID <> T2.ID
    and T1.LENGTH_BATCH_FK=T2.LENGTH_BATCH_FK
    and T1.SAMPLE_WEIGHT<>T2.SAMPLE_WEIGHT
    and T1.SAMPLE_WEIGHT_PMFM_FK<>T2.SAMPLE_WEIGHT_PMFM_FK
where T1.SAMPLE_WEIGHT_PMFM_FK=123 AND T1.SAMPLE_WEIGHT > T2.SAMPLE_WEIGHT;*/

-- Suppression des poids de reference en doublon (RTP + standard)
update QUANTIFICATION_MEASUREMENT_B set IS_REFERENCE_QUANTIFICATION=false where
    IS_REFERENCE_QUANTIFICATION = true AND PMFM_FK=123
                                                                            AND BATCH_FK IN (
        select distinct T1.SAMPLE_BATCH_FK from TMP_BATCH T1
                                                    inner join TMP_BATCH T2 on T1.ID <> T2.ID
            and T1.LENGTH_BATCH_FK=T2.LENGTH_BATCH_FK
            and T1.SAMPLE_WEIGHT<>T2.SAMPLE_WEIGHT
            and T1.SAMPLE_WEIGHT_PMFM_FK<>T2.SAMPLE_WEIGHT_PMFM_FK
        where 1=1
          AND T1.SAMPLE_WEIGHT_PMFM_FK=123
          AND T1.SAMPLE_WEIGHT > T2.SAMPLE_WEIGHT
    );

-- Suppression des poids dans la table temporaire
delete from TMP_BATCH where ID IN (
    select T1.ID from TMP_BATCH T1
                          inner join TMP_BATCH T2 on T1.ID <> T2.ID
        and T1.LENGTH_BATCH_FK=T2.LENGTH_BATCH_FK
        and T1.SAMPLE_WEIGHT<>T2.SAMPLE_WEIGHT
        and T1.SAMPLE_WEIGHT_PMFM_FK<>T2.SAMPLE_WEIGHT_PMFM_FK
    where 1=1
      AND T1.SAMPLE_WEIGHT_PMFM_FK=123
      AND T1.SAMPLE_WEIGHT > T2.SAMPLE_WEIGHT
);

-- Compute RWC to use
drop table TMP_RWC if exists;
drop sequence TMP_RWC_SEQ if exists;
create sequence TMP_RWC_SEQ AS INTEGER START WITH 1;
create table TMP_RWC as (
                            select
                                next value for TMP_RWC_SEQ as ID,
                                LENGTH_BATCH_FK,
                                CONVERSION_COEFFICIENT,
                                START_DATE,
                                END_DATE
                            from (
                                     select
                                         T.LENGTH_BATCH_FK,
                                         RWC.CONVERSION_COEFFICIENT as CONVERSION_COEFFICIENT,
                                         RWC.START_DATE as START_DATE,
                                         RWC.END_DATE as END_DATE
                                     from
                                         TMP_BATCH T,
                                         ROUND_WEIGHT_CONVERSION RWC
                                     where 1=1
                                       AND RWC.TAXON_GROUP_FK = T.TAXON_GROUP_FK
                                       AND RWC.DRESSING_FK = T.DRESSING_FK
                                       AND RWC.PRESERVING_FK = T.PRESERVATION_FK
                                       AND RWC.LOCATION_FK = 1 /*FRA*/
                                       AND RWC.STATUS_FK=1
                                     -- DEBUG
                                     --AND T.LENGTH_BATCH_FK=3369046
                                     ORDER BY RWC.START_DATE DESC
                                 )
) WITH DATA;
create index TMP__RWC_LENGTH_BATCH_FK ON TMP_RWC(LENGTH_BATCH_FK);

-- Suppression des RWC en doublon (en gardant les plus récents)
delete from TMP_RWC where ID IN (
    select T1.ID
    from TMP_RWC T1, TMP_RWC T2
    where T1.ID > T2.ID and T1.LENGTH_BATCH_FK=T2.LENGTH_BATCH_FK
);
--select count(*) from TMP_RWC; -- 22373

drop table TMP_RTP if exists ;
drop sequence TMP_RTP_SEQ if exists ;
create sequence TMP_RTP_SEQ AS INTEGER START WITH 1;
create table TMP_RTP as (
                            select
                                next value for TMP_RTP_SEQ as ID,
                                LENGTH_BATCH_FK,
                                CONVERSION_COEFFICIENT_A,
                                CONVERSION_COEFFICIENT_B,
                                FAO_ZONE,
                                YEAR,
                                START_MONTH,
                                END_MONTH,
                                SCORE
                            from (
                                     select T.LENGTH_BATCH_FK,
                                            WLC.CONVERSION_COEFFICIENT_A as CONVERSION_COEFFICIENT_A,
                                            WLC.CONVERSION_COEFFICIENT_B as CONVERSION_COEFFICIENT_B,
                                            L.LABEL                      AS FAO_ZONE,
                                            WLC.YEAR                     as YEAR,
                                            WLC.START_MONTH,
                                            WLC.END_MONTH,
                                            (CASE
                                                 WHEN (WLC.START_MONTH <= T.MONTH AND WLC.END_MONTH >= T.MONTH) THEN 1000
                                                 ELSE 0
                                                END) + WLC.YEAR          AS SCORE
                                     from TMP_BATCH T,
                                          WEIGHT_LENGTH_CONVERSION WLC,
                                          LOCATION L,
                                          LOCATION_HIERARCHY LH
                                     where 1 = 1
                                       AND L.ID = WLC.LOCATION_FK
                                       AND LH.CHILD_LOCATION_FK = T.RECTANGLE_FK
                                       AND WLC.REFERENCE_TAXON_FK = T.REFERENCE_TAXON_FK
                                       AND WLC.LOCATION_FK = LH.PARENT_LOCATION_FK
                                       AND WLC.LENGTH_PARAMETER_FK = T.LENGTH_PARAMETER_FK
                                       AND WLC.LENGTH_UNIT_FK = T.LENGTH_UNIT_FK
                                       AND WLC.STATUS_FK = 1
                                       AND WLC.SEX_QUALITATIVE_VALUE_FK=9325 /*Non sexé*/
                                     -- DEBUG
                                     --AND T.LENGTH_BATCH_FK = 3369046
                                     ORDER BY SCORE DESC
                                 )
) WITH DATA
;
create index TMP_RTP_LENGTH_BATCH_FK ON TMP_RTP(LENGTH_BATCH_FK);
-- Suppression des RTP en doublon (en gardant les plus récents)
delete from TMP_RTP where ID IN (
    select T1.ID
    from TMP_RTP T1, TMP_RTP T2
    where T1.ID > T2.ID and T1.LENGTH_BATCH_FK=T2.LENGTH_BATCH_FK
);
--select count(*) from TMP_RTP; 22373

-- Vérification: il ne doit y avoir aucun BATCH sans coefficient RWC ou RTP
/*
select T.*,
       RWC.ID AS RWC_ID,
       RTP.ID AS RTP_ID
from TMP_BATCH T
left outer join TMP_RWC RWC on RWC.LENGTH_BATCH_FK=T.LENGTH_BATCH_FK
left outer join TMP_RTP RTP on RTP.LENGTH_BATCH_FK=T.LENGTH_BATCH_FK
where RTP.ID IS NULL OR RWC.ID IS NULL;
*/

drop table TMP_FIX_INDIV if exists;
create table TMP_FIX_INDIV as (
                                  select
                                      T.LENGTH_BATCH_FK,
                                      T.RTP_WEIGHT AS OLD_RTP_WEIGHT,
                                      ROUND((RTP.CONVERSION_COEFFICIENT_A * POWER(T.LENGTH_VALUE+0.5, RTP.CONVERSION_COEFFICIENT_B))
                                                / RWC.CONVERSION_COEFFICIENT, 6) as NEW_RTP_WEIGHT
                                  from TMP_BATCH T
                                           inner join TMP_RWC RWC on RWC.LENGTH_BATCH_FK=T.LENGTH_BATCH_FK
                                           inner join TMP_RTP RTP on RTP.LENGTH_BATCH_FK=T.LENGTH_BATCH_FK
                                  where 1=1
    -- DEBUG
    --AND T.LENGTH_BATCH_FK in (3356032, 3356033)
) WITH DATA;
create index TMP_FIX_INDIV_LENGTH_BATCH_FK ON TMP_FIX_INDIV(LENGTH_BATCH_FK);
/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

--select count(*) from TMP_FIX_INDIV;

-- DEBUG
--select * from TMP_BATCH where LENGTH_BATCH_FK in (3356032, 3356033);
--select * from TMP_BATCH where SAMPLE_BATCH_FK = 3356031;
--select * from TMP_RTP where LENGTH_BATCH_FK in (3356032, 3356033);
--select * from TMP_FIX_INDIV where LENGTH_BATCH_FK in (3356032, 3356033);
-- DEBUG - Nb poids RTP indiv avec delta >= 1g
select count(*), T.YEAR
from TMP_FIX_INDIV FIX_INDIV inner join TMP_BATCH T on T.LENGTH_BATCH_FK=FIX_INDIV.LENGTH_BATCH_FK
where ABS(FIX_INDIV.NEW_RTP_WEIGHT - FIX_INDIV.OLD_RTP_WEIGHT) >= 0.001
group by T.YEAR;

drop table TMP_FIX_SAMPLE if exists;
create table TMP_FIX_SAMPLE as (
                                   select
                                       T.SAMPLE_BATCH_FK,
                                       MIN(T.SAMPLE_WEIGHT) as OLD_SAMPLE_WEIGHT,
                                       SUM(FIX_INDIV.OLD_RTP_WEIGHT) as OLD_SAMPLE_RTP_WEIGHT,
                                       SUM(FIX_INDIV.NEW_RTP_WEIGHT) as NEW_SAMPLE_RTP_WEIGHT
                                   from TMP_FIX_INDIV FIX_INDIV inner join TMP_BATCH T on T.LENGTH_BATCH_FK=FIX_INDIV.LENGTH_BATCH_FK
                                   where 1=1
                                   -- DEBUG
                                   --AND T.LENGTH_BATCH_FK=3369046
                                   group by T.SAMPLE_BATCH_FK
) WITH DATA;
create index TMP_FIX_SAMPLE_BATCH_FK ON TMP_FIX_SAMPLE(SAMPLE_BATCH_FK);
--select count(*) from TMP_FIX_SAMPLE; -- 1471

-- DEBUG - Nb poids échant avec delta >= 1g
select count(distinct FIX_SAMPLE.SAMPLE_BATCH_FK), T.YEAR
from TMP_FIX_SAMPLE FIX_SAMPLE inner join TMP_BATCH T on T.SAMPLE_BATCH_FK=FIX_SAMPLE.SAMPLE_BATCH_FK
where ABS(FIX_SAMPLE.NEW_SAMPLE_RTP_WEIGHT - FIX_SAMPLE.OLD_SAMPLE_RTP_WEIGHT) >= 0.001
group by T.YEAR
;

-- DEBUG - reset previous computed weight
UPDATE TMP_BATCH B set NEW_RTP_WEIGHT = null, NEW_SAMPLE_RTP_WEIGHT= null;

-- Fill NEW_RTP_WEIGHT
UPDATE TMP_BATCH B set NEW_RTP_WEIGHT=(select FIX_INDIV.NEW_RTP_WEIGHT from TMP_FIX_INDIV FIX_INDIV where FIX_INDIV.LENGTH_BATCH_FK=B.LENGTH_BATCH_FK);

-- Keep only rows when RTP_WEIGHT <> OLD_RTP_WEIGHT
delete from TMP_BATCH where RTP_WEIGHT = NEW_RTP_WEIGHT; -- 2844 removed
--select count(*) from TMP_BATCH; -- 19529

-- Fill NEW_SAMPLE_WEIGHT
--UPDATE TMP_BATCH T set NEW_SAMPLE_RTP_WEIGHT = null;
UPDATE TMP_BATCH T set NEW_SAMPLE_RTP_WEIGHT = (select FIX_SAMPLE.NEW_SAMPLE_RTP_WEIGHT from TMP_FIX_SAMPLE FIX_SAMPLE where FIX_SAMPLE.SAMPLE_BATCH_FK=T.SAMPLE_BATCH_FK)
where T.SAMPLE_BATCH_FK in (select SAMPLE_BATCH_FK from TMP_FIX_SAMPLE);

-- UPDATE TMP_BATCH T set NEW_SAMPLE_RTP_WEIGHT = null
-- WHERE T.SAMPLING_RATIO <> 1
--   OR T.SAMPLING_RATIO_TEXT not like '%/'
-- Only computed sampling ratio

-- DEBUG
--select * from TMP_BATCH where LENGTH_BATCH_FK=3356032;
--select * from TMP_BATCH where SAMPLE_BATCH_FK=3356031;
--    AND T.SAMPLING_RATIO < 1 AND T.SAMPLING_RATIO_TEXT like '%/%' -- Only computed sampling ratio


--select RTP_WEIGHT, NEW_RTP_WEIGHT from TMP_BATCH where SAMPLE_BATCH_FK=3369045;
--select SAMPLING_RATIO, SAMPLE_WEIGHT, NEW_SAMPLE_WEIGHT from TMP_BATCH where SAMPLE_BATCH_FK=3369045;

-- Mise à jour poids RTP
/*
UPDATE QUANTIFICATION_MEASUREMENT_B QM set NUMERICAL_VALUE=
    (select NEW_RTP_WEIGHT from TMP_BATCH where RTP_MEASUREMENT_ID=QM.ID)
where QM.ID in (select RTP_MEASUREMENT_ID from TMP_BATCH where NEW_RTP_WEIGHT IS NOT NULL) AND QM.IS_REFERENCE_QUANTIFICATION=true;

UPDATE QUANTIFICATION_MEASUREMENT_B QM set NUMERICAL_VALUE=
    (select NEW_SAMPLE_WEIGHT from TMP_BATCH where SAMPLE_WEIGHT_MEASUREMENT_ID=QM.ID)
where QM.ID in (select SAMPLE_WEIGHT_MEASUREMENT_ID from TMP_BATCH where NEW_SAMPLE_WEIGHT IS NOT NULL) AND QM.IS_REFERENCE_QUANTIFICATION=true;

*/


/*
UPDATE QUANTIFICATION_MEASUREMENT_B QM set NUMERICAL_VALUE=
                                               (select T.NEW_RTP_SAMPLE_WEIGHT from TMP_FIX_SAMPLE T where T.SAMPLE_BATCH_FK=QM.BATCH_FK)
where QM.ID in (select TMP_BATCH.SAMPLE_WEIGHT_MEASUREMENT_ID from TMP_BATCH) AND QM.IS_REFERENCE_QUANTIFICATION=true;
*/


-- DROP temporary tables + sequences
/*
drop table TMP_BATCH if exists;
drop table TMP_RWC if exists;
drop table TMP_RTP if exists;
drop table TMP_FIX_SAMPLE if exists;
drop table TMP_FIX_INDIV if exists;
drop sequence TMP_BATCH_SEQ if exists;
drop sequence TMP_RWC_SEQ if exists;
drop sequence TMP_RTP_SEQ if exists;
*/

-- Comptage des lignes impactées
-- select count(distinct T.TRIP_FK), T.YEAR TRIP_COUNT from TMP_BATCH T group by YEAR; -- 133 trips
-- select count(distinct T.OPERATION_FK), T.YEAR from TMP_BATCH T group by YEAR;-- 1203 OP
-- select count(distinct T.SAMPLE_BATCH_FK) TRIP_COUNT from TMP_BATCH T; -- 1471 Lots espèces
-- select
--     count(distinct T.LENGTH_BATCH_FK) LENGTH_BATCH_COUNT,
--     T.TAXON_GROUP_LABEL,
--     T.REFERENCE_TAXON_LABEL,
--     T.DRESSING_LABEL,
--     T.PRESERVATION_LABEL
-- from TMP_BATCH T
-- group by T.TAXON_GROUP_LABEL, T.REFERENCE_TAXON_LABEL, T.DRESSING_LABEL, T.PRESERVATION_LABEL;


-- Comptage des SAMPLE_BATCH où le delta avec l'ancien poids est > 1g
/*select count(distinct FIX_SAMPLE.SAMPLE_BATCH_FK), T.YEAR
from TMP_FIX_SAMPLE FIX_SAMPLE inner join TMP_BATCH T on T.SAMPLE_BATCH_FK=FIX_SAMPLE.SAMPLE_BATCH_FK
where 1=1
  AND ABS(FIX_SAMPLE.NEW_RTP_SAMPLE_WEIGHT - FIX_SAMPLE.OLD_RTP_SAMPLE_WEIGHT) > 0
  --AND FIX_SAMPLE.NEW_SAMPLE_WEIGHT > FIX_SAMPLE.OLD_SAMPLE_WEIGHT
  AND T.SAMPLING_RATIO < 1
group by T.YEAR
;*/

-- DEBUG
-- select * from TMP_BATCH where SAMPLE_BATCH_FK=3369045; -- Echant COD
-- select * from TMP_BATCH where LENGTH_BATCH_FK=3369046; -- Indiv de 70 cm
-- select * from TMP_RWC where LENGTH_BATCH_FK=3369046;
-- select * from TMP_RTP where LENGTH_BATCH_FK=3369046;
-- select * from TMP_FIX_INDIV where LENGTH_BATCH_FK=3369046;
-- select * from TMP_FIX_SAMPLE where SAMPLE_BATCH_FK=3369045;
--select * from TMP_BATCH where SAMPLE_BATCH_FK=3369045;
