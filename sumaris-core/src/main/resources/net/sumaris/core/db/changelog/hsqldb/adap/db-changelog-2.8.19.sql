--create index LOCATION_LABEL_IDX on LOCATION(LABEL);

drop table TMP_BATCH if exists;
drop sequence TMP_BATCH_SEQ IF EXISTS ;
create sequence TMP_BATCH_SEQ AS INTEGER START WITH 1;
create table TMP_BATCH
as (
    select
      NEXT VALUE FOR TMP_BATCH_SEQ                 as ID,
      T.ID                                         as TRIP_FK,
      O.ID                                         as OPERATION_FK,
      extract(year from O.END_DATE_TIME)           as YEAR,
      extract(month from O.END_DATE_TIME)          as MONTH,
      O.END_DATE_TIME                              as DATE_TIME,
      L_RECT.ID                                    as RECTANGLE_FK,
      L_RECT.LABEL                                 as RECTANGLE_LABEL,
      TG.ID                                        as TAXON_GROUP_FK,
      TG.LABEL                                     AS TAXON_GROUP_LABEL,
      LANDING_QM.ID                                as TOTAL_WEIGHT_MEASUREMENT_ID,
      LANDING_QM.PMFM_FK                           as TOTAL_WEIGHT_PMFM_FK,
      LANDING_QM.NUMERICAL_VALUE                   as TOTAL_WEIGHT,
      SAMPLE_B.ID                                  as SAMPLE_BATCH_FK,
      SAMPLE_B.SAMPLING_RATIO                      as SAMPLING_RATIO,
      SAMPLE_B.SAMPLING_RATIO_TEXT                 as SAMPLING_RATIO_TEXT,
      cast(null as DOUBLE)                         as NEW_SAMPLING_RATIO,      -- Will be computed later
      cast(null as VARCHAR(50))                    as NEW_SAMPLING_RATIO_TEXT, -- Will be computed later
      SAMPLE_QM.ID                                 as SAMPLE_WEIGHT_MEASUREMENT_ID,
      SAMPLE_QM.PMFM_FK                            as SAMPLE_WEIGHT_PMFM_FK,
      SAMPLE_QM.NUMERICAL_VALUE                    as SAMPLE_WEIGHT,
      cast(null as DOUBLE)                         as OLD_SAMPLE_RTP_WEIGHT,   -- Will be computed later
      cast(null as DOUBLE)                         as NEW_SAMPLE_WEIGHT,       -- Will be computed later
      LENGTH_B.ID                                  as LENGTH_BATCH_FK,
      LENGTH_B.INDIVIDUAL_COUNT                    as LENGTH_INDIVIDUAL_COUNT,
      LENGTH_B.REFERENCE_TAXON_FK                  as REFERENCE_TAXON_FK,
      TN.LABEL                                     as REFERENCE_TAXON_LABEL,
      LENGTH_QM.ID                                 as RTP_MEASUREMENT_ID,
      LENGTH_QM.NUMERICAL_VALUE                    as RTP_WEIGHT,
      cast(null as DOUBLE)                         as NEW_RTP_WEIGHT,          -- Will be computed later
      LENGTH_PMFM.ID                               as LENGTH_PMFM_FK,
      LENGTH_PMFM.PARAMETER_FK                     as LENGTH_PARAMETER_FK,
      LENGTH_PMFM.UNIT_FK                          as LENGTH_UNIT_FK,
      LENGTH_SM.NUMERICAL_VALUE                    as LENGTH_VALUE,
      CASE
          WHEN DRESSING_QV.ID IS NOT NULL then DRESSING_QV.ID
          WHEN LANDING_SM.QUALITATIVE_VALUE_FK = 190 THEN 339 -- GUT by default if Landing
          WHEN LANDING_SM.QUALITATIVE_VALUE_FK = 191 THEN 331 -- WHL by default if Discard
          END                                      as DRESSING_FK,
      CASE
          WHEN DRESSING_QV.LABEL IS NOT NULL then DRESSING_QV.LABEL
          WHEN LANDING_SM.QUALITATIVE_VALUE_FK = 190 THEN 'GUT' -- GUT by default if Landing
          WHEN LANDING_SM.QUALITATIVE_VALUE_FK = 191 THEN 'WHL' -- WHL by default if Discard
          END                                      as DRESSING_LABEL,
      COALESCE(PRESERVATION_QV.ID, 367)            as PRESERVATION_FK, -- FRE by default
      COALESCE(PRESERVATION_QV.LABEL, 'FRE')       as PRESERVATION_LABEL
    from BATCH LENGTH_B
            inner join QUANTIFICATION_MEASUREMENT_B LENGTH_QM
                       on LENGTH_QM.BATCH_FK = LENGTH_B.ID AND LENGTH_QM.PMFM_FK = 122 -- RTP WEIGHT
                           AND LENGTH_QM.IS_REFERENCE_QUANTIFICATION = true
            inner join SORTING_MEASUREMENT_B LENGTH_SM on LENGTH_SM.BATCH_FK = LENGTH_B.ID AND
                                                          LENGTH_SM.NUMERICAL_VALUE IS NOT NULL -- LENGTH_SM.PMFM_FK=81 -- LENGTH
            inner join PMFM LENGTH_PMFM on LENGTH_PMFM.ID = LENGTH_SM.PMFM_FK
            inner join OPERATION O on O.ID = LENGTH_B.OPERATION_FK
            inner join TRIP T on T.ID = O.TRIP_FK
            inner join BATCH SAMPLE_B on SAMPLE_B.ID = LENGTH_B.PARENT_BATCH_FK
            inner join BATCH LANDING_B on LANDING_B.ID = SAMPLE_B.PARENT_BATCH_FK
            inner join VESSEL_POSITION VP on VP.OPERATION_FK = O.ID and VP.DATE_TIME = O.END_DATE_TIME
            inner join LOCATION L_RECT
                       ON L_RECT.LABEL = F_TO_RECTANGLE(VP.LATITUDE, VP.LONGITUDE) AND
                          L_RECT.LOCATION_LEVEL_FK in (4, 5) /*rect stat*/
            inner join SORTING_MEASUREMENT_B LANDING_SM
                       on LANDING_SM.BATCH_FK = LANDING_B.ID AND
                          LANDING_SM.QUALITATIVE_VALUE_FK in (190, 191) -- LANDING OR DISCARD
            inner join BATCH SPECIES_B on SPECIES_B.ID = LANDING_B.PARENT_BATCH_FK AND
                                          SPECIES_B.TAXON_GROUP_FK IS NOT NULL
            inner join TAXON_NAME TN ON TN.REFERENCE_TAXON_FK = LENGTH_B.REFERENCE_TAXON_FK AND
                                        TN.IS_REFERENT = true
            inner join TAXON_GROUP TG ON TG.ID = SPECIES_B.TAXON_GROUP_FK
            left outer join SORTING_MEASUREMENT_B DRESSING_B
                            on DRESSING_B.BATCH_FK = LANDING_B.ID AND DRESSING_B.PMFM_FK = 151 -- DRESSING
            left outer join QUALITATIVE_VALUE DRESSING_QV
                            ON DRESSING_QV.ID = DRESSING_B.QUALITATIVE_VALUE_FK
            left outer join QUANTIFICATION_MEASUREMENT_B SAMPLE_QM
                            on SAMPLE_QM.BATCH_FK = SAMPLE_B.ID AND
                               SAMPLE_QM.IS_REFERENCE_QUANTIFICATION = true
            left outer join QUANTIFICATION_MEASUREMENT_B LANDING_QM
                            on LANDING_QM.BATCH_FK = LANDING_B.ID AND
                               LANDING_QM.IS_REFERENCE_QUANTIFICATION = true
            left outer join SORTING_MEASUREMENT_B PRESERVATION_B
                            on PRESERVATION_B.BATCH_FK = LANDING_B.ID AND PRESERVATION_B.PMFM_FK = 150 -- PRESERVATION
            left outer join QUALITATIVE_VALUE PRESERVATION_QV
                            ON PRESERVATION_QV.ID = PRESERVATION_B.QUALITATIVE_VALUE_FK
    where LENGTH_B.LABEL LIKE 'SORTING_BATCH_INDIVIDUAL#%'
) WITH DATA;

create index TMP_BATCH_LENGTH_BATCH_FK ON TMP_BATCH(LENGTH_BATCH_FK);
create index TMP_BATCH_SAMPLE_BATCH_FK ON TMP_BATCH(SAMPLE_BATCH_FK);
create index TMP_BATCH_RTP_MEASUREMENT_ID ON TMP_BATCH(RTP_MEASUREMENT_ID);

-- Replace PRESERVATION 'ALI - Alive' with 'FRE - Fresh'
update TMP_BATCH set PRESERVATION_FK=367, PRESERVATION_LABEL='FRE' where PRESERVATION_FK=368;

-- Replace DRESSING 'GTA - Eviscéré et équeuté' with 'GUT - Eviscéré'
update TMP_BATCH set DRESSING_FK=339, DRESSING_LABEL='GUT' where DRESSING_FK=338;

-- Analyse des poids de reference en doublon (RTP + standard)
/*select T1.* from TMP_BATCH T1
                     inner join TMP_BATCH T2 on T1.ID <> T2.ID
    and T1.LENGTH_BATCH_FK=T2.LENGTH_BATCH_FK
    and T1.SAMPLE_WEIGHT<>T2.SAMPLE_WEIGHT
    and T1.SAMPLE_WEIGHT_PMFM_FK<>T2.SAMPLE_WEIGHT_PMFM_FK
where T1.SAMPLE_WEIGHT_PMFM_FK=123;*/

-- Suppression des poids de reference en doublon (RTP + standard)
drop table TMP_SAMPLE_WEIGHT_REDUNDANT if exists;
create table TMP_SAMPLE_WEIGHT_REDUNDANT
as (
    select distinct
        T1.ID as ID,
        T1.TRIP_FK as TRIP_FK,
        T1.OPERATION_FK as OPERATION_FK,
        T1.SAMPLE_BATCH_FK as SAMPLE_BATCH_FK,
        T1.SAMPLE_WEIGHT_MEASUREMENT_ID as SAMPLE_WEIGHT_MEASUREMENT_ID
    from TMP_BATCH T1
             inner join TMP_BATCH T2 on T1.ID <> T2.ID
        and T1.LENGTH_BATCH_FK=T2.LENGTH_BATCH_FK
        and T1.SAMPLE_WEIGHT<>T2.SAMPLE_WEIGHT
        and T1.SAMPLE_WEIGHT_PMFM_FK<>T2.SAMPLE_WEIGHT_PMFM_FK
    where 1=1
      AND T1.SAMPLE_WEIGHT_PMFM_FK=123
) WITH DATA;
--select count(*) from TMP_SAMPLE_WEIGHT_REDUNDANT; -- 109 doublons

-- Suppression des poids dans la table temporaire
DELETE FROM TMP_BATCH where ID IN (select ID from TMP_SAMPLE_WEIGHT_REDUNDANT);

--select count(*) from TMP_BATCH; -- 29278 rows

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
                                       AND RWC.STATUS_FK <> 0
                                     -- DEBUG
                                     --AND T.LENGTH_BATCH_FK=3369046
                                     ORDER BY RWC.START_DATE DESC
                                 )
) WITH DATA;
create index TMP_RWC_LENGTH_BATCH_FK ON TMP_RWC(LENGTH_BATCH_FK);

-- Suppression des RWC en doublon (en gardant les plus récents)
delete from TMP_RWC where ID IN (
    select T1.ID
    from TMP_RWC T1, TMP_RWC T2
    where T1.ID > T2.ID and T1.LENGTH_BATCH_FK=T2.LENGTH_BATCH_FK
);
--select count(*) from TMP_RWC; -- 29278

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
                                       AND WLC.STATUS_FK <> 0
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
--select count(*) from TMP_RTP; 29278

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
create table TMP_FIX_INDIV
as (
    select
      T.LENGTH_BATCH_FK,
      T.RTP_WEIGHT AS OLD_RTP_WEIGHT,
      ROUND((RTP.CONVERSION_COEFFICIENT_A * POWER(T.LENGTH_VALUE+0.5, RTP.CONVERSION_COEFFICIENT_B))
                / RWC.CONVERSION_COEFFICIENT, 6)
          * COALESCE(T.LENGTH_INDIVIDUAL_COUNT, 1)
                   as NEW_RTP_WEIGHT
    from TMP_BATCH T
           inner join TMP_RWC RWC on RWC.LENGTH_BATCH_FK=T.LENGTH_BATCH_FK
           inner join TMP_RTP RTP on RTP.LENGTH_BATCH_FK=T.LENGTH_BATCH_FK
    where 1=1
    -- DEBUG
    --AND T.LENGTH_BATCH_FK in (3356032, 3356033)
) WITH DATA;
create index TMP_FIX_INDIV_LENGTH_BATCH_FK ON TMP_FIX_INDIV(LENGTH_BATCH_FK);

--select count(*) from TMP_FIX_INDIV;

-- DEBUG - Nb poids RTP indiv avec delta >= 1g
/*select count(*), T.YEAR
from TMP_FIX_INDIV FIX_INDIV inner join TMP_BATCH T on T.LENGTH_BATCH_FK=FIX_INDIV.LENGTH_BATCH_FK
where ABS(FIX_INDIV.NEW_RTP_WEIGHT - FIX_INDIV.OLD_RTP_WEIGHT) >= 0.001
group by T.YEAR;*/

drop table TMP_FIX_SAMPLE if exists;
create table TMP_FIX_SAMPLE
as (
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
--select count(*) from TMP_FIX_SAMPLE; -- 2171

-- Keep only samples that need to be fix
DELETE from TMP_FIX_SAMPLE
where
   ABS(OLD_SAMPLE_RTP_WEIGHT - OLD_SAMPLE_WEIGHT) > 0.01 -- Samples NOT using the SUM(RTP)
   OR ABS(NEW_SAMPLE_RTP_WEIGHT - OLD_SAMPLE_RTP_WEIGHT) < 0.001 -- Samples already using the correct weight (at 1g)
;
--select count(*) from TMP_FIX_SAMPLE; -- 179 samples to fix

-- DEBUG - reset previous computed weight
UPDATE TMP_BATCH B set NEW_RTP_WEIGHT = null, NEW_SAMPLE_WEIGHT= null, NEW_SAMPLING_RATIO=null, NEW_SAMPLING_RATIO_TEXT=null, OLD_SAMPLE_RTP_WEIGHT=null;

-- Fill NEW_RTP_WEIGHT
UPDATE TMP_BATCH B set NEW_RTP_WEIGHT=(select FIX_INDIV.NEW_RTP_WEIGHT from TMP_FIX_INDIV FIX_INDIV where FIX_INDIV.LENGTH_BATCH_FK=B.LENGTH_BATCH_FK);

-- Keep only rows when RTP_WEIGHT <> OLD_RTP_WEIGHT
delete from TMP_BATCH where RTP_WEIGHT = NEW_RTP_WEIGHT; -- 8446 removed
--select count(*) from TMP_BATCH; -- 20832

-- Fill OLD_SAMPLE_RTP_WEIGHT, using the old/bad SUM(RTP)
UPDATE TMP_BATCH T set OLD_SAMPLE_RTP_WEIGHT = (select FIX_SAMPLE.OLD_SAMPLE_RTP_WEIGHT from TMP_FIX_SAMPLE FIX_SAMPLE where FIX_SAMPLE.SAMPLE_BATCH_FK=T.SAMPLE_BATCH_FK)
where T.SAMPLE_BATCH_FK in (select SAMPLE_BATCH_FK from TMP_FIX_SAMPLE);

-- Fill NEW_SAMPLE_WEIGHT, using the fixed SUM(RTP)
UPDATE TMP_BATCH T set NEW_SAMPLE_WEIGHT = (select FIX_SAMPLE.NEW_SAMPLE_RTP_WEIGHT from TMP_FIX_SAMPLE FIX_SAMPLE where FIX_SAMPLE.SAMPLE_BATCH_FK=T.SAMPLE_BATCH_FK)
where T.SAMPLE_BATCH_FK in (select SAMPLE_BATCH_FK from TMP_FIX_SAMPLE);

-- Fill NEW_SAMPLING_RATIO and NEW_SAMPLING_RATIO_TEXT
UPDATE TMP_BATCH T
set NEW_SAMPLING_RATIO = ROUND(T.NEW_SAMPLE_WEIGHT / T.TOTAL_WEIGHT, 6),
    NEW_SAMPLING_RATIO_TEXT = CAST(ROUND(T.NEW_SAMPLE_WEIGHT, 3) AS DECIMAL(18,3)) || '/' || CAST(ROUND(T.TOTAL_WEIGHT, 3) AS DECIMAL(18,3));

-- Force NEW_SAMPLING_RATIO=1 and SAMPLE_WEIGHT=TOTAL_WEIGHT if computed sampling ratio > 1
UPDATE TMP_BATCH T set NEW_SAMPLING_RATIO=1, NEW_SAMPLE_WEIGHT=T.TOTAL_WEIGHT, NEW_SAMPLING_RATIO_TEXT=T.TOTAL_WEIGHT || '/' || T.TOTAL_WEIGHT
where
   -- Poids saisi uniquement
    T.TOTAL_WEIGHT_PMFM_FK in (91,92)
    -- Taux 100% (calculé ou non) ou nouveau taux > 1
    AND SAMPLING_RATIO = 1 OR (NEW_SAMPLING_RATIO >= 0.8 AND SAMPLING_RATIO >= 0.8);

-- Keep existing sample weight + ratio, if was set manually (no row match this case, in dataset [2022-2023])
UPDATE TMP_BATCH T set NEW_SAMPLE_WEIGHT=T.SAMPLE_WEIGHT, NEW_SAMPLING_RATIO=T.SAMPLING_RATIO, NEW_SAMPLING_RATIO_TEXT=T.SAMPLING_RATIO_TEXT where TOTAL_WEIGHT_PMFM_FK not in (91,92) OR SAMPLING_RATIO_TEXT not like '%/%';

-- Create a table with all sample to update
drop table TMP_SAMPLE if exists;
create table TMP_SAMPLE
as (
    select distinct
       YEAR,
       SAMPLE_BATCH_FK,
       SAMPLING_RATIO,
       SAMPLING_RATIO_TEXT,
       NEW_SAMPLING_RATIO,
       NEW_SAMPLING_RATIO_TEXT,
       SAMPLE_WEIGHT,
       NEW_SAMPLE_WEIGHT,
       SAMPLE_WEIGHT_MEASUREMENT_ID
    from TMP_BATCH T
    where (NEW_SAMPLING_RATIO <> SAMPLING_RATIO OR NEW_SAMPLE_WEIGHT <> SAMPLE_WEIGHT)
) WITH DATA;
create index TMP_SAMPLE_BATCH_FK on TMP_SAMPLE(SAMPLE_BATCH_FK);
create index TMP_SAMPLE_WEIGHT_MEASUREMENT_ID on TMP_SAMPLE(SAMPLE_WEIGHT_MEASUREMENT_ID);
--select count(*) from TMP_SAMPLE; -- 156

-- ---------------------------------------------------------------------------------------------------------------------
-- MISE A JOUR DES BATCH
-- ---------------------------------------------------------------------------------------------------------------------
-- /!\ IMPORTANT: Mise à jour poids RTP individuel
UPDATE QUANTIFICATION_MEASUREMENT_B QM
set NUMERICAL_VALUE=(select NEW_RTP_WEIGHT from TMP_BATCH where RTP_MEASUREMENT_ID=QM.ID)
where QM.ID in (select RTP_MEASUREMENT_ID from TMP_BATCH where NEW_RTP_WEIGHT IS NOT NULL);
-- 20 832 rows affected

-- /!\ IMPORTANT: Mise à jour des taux d'échantillonnage
UPDATE BATCH S
set SAMPLING_RATIO=(select NEW_SAMPLING_RATIO from TMP_SAMPLE where SAMPLE_BATCH_FK=S.ID),
    SAMPLING_RATIO_TEXT=(select NEW_SAMPLING_RATIO_TEXT from TMP_SAMPLE where SAMPLE_BATCH_FK=S.ID),
    HASH=null
where S.ID in (select SAMPLE_BATCH_FK from TMP_SAMPLE where NEW_SAMPLING_RATIO IS NOT NULL AND NEW_SAMPLING_RATIO_TEXT IS NOT NULL);
-- 156 rows affected

-- /!\ IMPORTANT: Mise à jour des poids échantillonné
UPDATE QUANTIFICATION_MEASUREMENT_B QM
set NUMERICAL_VALUE=(select ROUND(NEW_SAMPLE_WEIGHT, 3) from TMP_SAMPLE where SAMPLE_WEIGHT_MEASUREMENT_ID=QM.ID)
where QM.ID in (select SAMPLE_WEIGHT_MEASUREMENT_ID from TMP_SAMPLE where NEW_SAMPLE_WEIGHT IS NOT NULL);
-- 156 rows affected

-- /!\ IMPORTANT: Remove duplicated RTP weight
delete from QUANTIFICATION_MEASUREMENT_B WHERE ID in (select distinct SAMPLE_WEIGHT_MEASUREMENT_ID FROM TMP_SAMPLE_WEIGHT_REDUNDANT);
-- 109 rows affected

-- ---------------------------------------------------------------------------------------------------------------------
-- MISE A JOUR DES PARENTS (TRIP, OPERATION, CATCH_BATCH)
-- ---------------------------------------------------------------------------------------------------------------------
UPDATE BATCH set update_date=current_timestamp, hash=null where BATCH.ID in (select distinct LENGTH_BATCH_FK from TMP_BATCH UNION select distinct SAMPLE_BATCH_FK from TMP_SAMPLE UNION select distinct SAMPLE_BATCH_FK from TMP_SAMPLE_WEIGHT_REDUNDANT);
UPDATE BATCH set update_date=current_timestamp, hash=null where BATCH.PARENT_BATCH_FK IS NULL AND OPERATION_FK in (select distinct OPERATION_FK from TMP_BATCH UNION select distinct OPERATION_FK from TMP_SAMPLE_WEIGHT_REDUNDANT);
UPDATE OPERATION set update_date=current_timestamp where ID in (select distinct OPERATION_FK from TMP_BATCH UNION select distinct OPERATION_FK from TMP_SAMPLE_WEIGHT_REDUNDANT);
UPDATE TRIP set update_date=current_timestamp where ID in (select distinct TRIP_FK from TMP_BATCH UNION select distinct TRIP_FK from TMP_SAMPLE_WEIGHT_REDUNDANT);

-- FINAL COMMIT
COMMIT;

-- ---------------------------------------------------------------------------------------------------------------------
-- DEBUG - Analyse, Comptage, etc
-- ---------------------------------------------------------------------------------------------------------------------

-- DEBUG: Stats sur les batch length corrigés
-- select count(distinct LENGTH_BATCH_FK) from TMP_BATCH;
-- DEBUG: Stats sur les samples corrigés
/*select
    count(distinct SAMPLE_BATCH_FK),
    min(ABS(NEW_SAMPLING_RATIO * 100 - SAMPLING_RATIO * 100)) as MIN_DELTA_SAMPLING_RATIO_PCT,
    max(ABS(NEW_SAMPLING_RATIO * 100 - SAMPLING_RATIO * 100)) as MAX_DELTA_SAMPLING_RATIO_PCT,
    min(ABS(NEW_SAMPLE_WEIGHT - SAMPLE_WEIGHT)) as MIN_DELTA_SAMPLE_WEIGHT,
    max(ABS(NEW_SAMPLE_WEIGHT - SAMPLE_WEIGHT)) as MAX_DELTA_SAMPLE_WEIGHT,
    sum(ABS(NEW_SAMPLE_WEIGHT - SAMPLE_WEIGHT)) as SUM_DELTA_SAMPLE_WEIGHT
from TMP_SAMPLE;*/

-- DEBUG
-- select * from TMP_BATCH where SAMPLE_BATCH_FK=3384031;
-- select LENGTH_VALUE, RTP_WEIGHT, NEW_RTP_WEIGHT from TMP_BATCH where SAMPLE_BATCH_FK=3384031;
-- select SAMPLING_RATIO, SAMPLE_WEIGHT, NEW_SAMPLE_WEIGHT from TMP_BATCH where SAMPLE_BATCH_FK=3384031;
-- select * from TMP_FIX_SAMPLE where SAMPLE_BATCH_FK=3384031;
-- select * from TMP_SAMPLE where SAMPLE_BATCH_FK=3384031;


-- ---------------------------------------------------------------------------------------------------------------------
-- CLEAN Clean up temporary objects
-- ---------------------------------------------------------------------------------------------------------------------

-- DROP temporary tables
drop table TMP_BATCH if exists;
drop table TMP_SAMPLE if exists;
drop table TMP_RWC if exists;
drop table TMP_RTP if exists;
drop table TMP_FIX_SAMPLE if exists;
drop table TMP_FIX_INDIV if exists;
drop table TMP_SAMPLE_WEIGHT_REDUNDANT if exists;

-- DROP temporary sequences
drop sequence TMP_BATCH_SEQ if exists;
drop sequence TMP_RWC_SEQ if exists;
drop sequence TMP_RTP_SEQ if exists;
