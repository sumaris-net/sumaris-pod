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

CREATE TABLE EXT_TR_425648064 NOLOGGING AS
SELECT DISTINCT
    UPPER('S') "SAMPLING_TYPE",
    PL.LABEL "LANDING_COUNTRY",
    VL.LABEL "VESSEL_FLAG_COUNTRY",
    CAST (EXTRACT(YEAR FROM T.RETURN_DATE_TIME) AS INT) "YEAR",
    P.LABEL "PROJECT",
    T.ID "TRIP_CODE",
    VF.LENGTH_OVER_ALL / 100 "VESSEL_LENGTH",
    VF.ADMINISTRATIVE_POWER "VESSEL_POWER",
    CAST(FLOOR(COALESCE(VF.GROSS_TONNAGE_GT, VF.GROSS_TONNAGE_GRT) / 100) AS INT) "VESSEL_SIZE",
    4 "VESSEL_TYPE",
    L.LABEL "HARBOUR",
    COALESCE(
            (SELECT CAST(ROUND(VUM.NUMERICAL_VALUE) AS INTEGER) FROM VESSEL_USE_MEASUREMENT VUM WHERE VUM.TRIP_FK=T.ID and VUM.PMFM_FK=-1),
            (SELECT COUNT(O.ID) FROM OPERATION O WHERE O.TRIP_FK=T.ID)
    ) "NUMBER_OF_SETS",
    FLOOR(T.RETURN_DATE_TIME - T.DEPARTURE_DATE_TIME) "DAYS_AT_SEA",
    T.VESSEL_FK "VESSEL_IDENTIFIER",
    COALESCE(DL.LABEL, PL.LABEL) "SAMPLING_COUNTRY",
    (CASE UPPER('Observer') WHEN 'SELFSAMPLING' THEN 'SelfSampling' ELSE 'Observer' END) "SAMPLING_METHOD",
    T.DEPARTURE_DATE_TIME "DEPARTURE_DATE_TIME",
    T.RETURN_DATE_TIME "RETURN_DATE_TIME",
    QUALITY_FLAG.NAME "QUALITY_FLAG",
    T.COMMENTS "TRIP_COMMENTS",
    MAX(CASE VUM.PMFM_FK WHEN 350 THEN VUM.ALPHANUMERICAL_VALUE ELSE NULL END) "LICENSEE_NAME",
    MAX(CASE VUM.PMFM_FK WHEN 351 THEN VUM.ALPHANUMERICAL_VALUE ELSE NULL END) "LICENSE_NUMBER",
    MAX(CASE VUM.PMFM_FK WHEN 1 THEN VUM.NUMERICAL_VALUE ELSE NULL END) "CREW_SIZE"
FROM
    TRIP T
        INNER JOIN PROGRAM P ON P.ID = T.PROGRAM_FK
        INNER JOIN LOCATION L ON L.ID = T.RETURN_LOCATION_FK
        INNER JOIN LOCATION_HIERARCHY LH on LH.CHILD_LOCATION_FK = T.RETURN_LOCATION_FK
        INNER JOIN LOCATION PL ON PL.ID = LH.PARENT_LOCATION_FK AND PL.LOCATION_LEVEL_FK = 21
        LEFT OUTER JOIN VESSEL_REGISTRATION_PERIOD VRP ON VRP.VESSEL_FK = T.VESSEL_FK
        AND VRP.START_DATE <= T.DEPARTURE_DATE_TIME
        AND COALESCE(VRP.END_DATE, to_date('01-01-2100','DD-MM-YYYY')) >= T.DEPARTURE_DATE_TIME
        LEFT OUTER JOIN LOCATION_HIERARCHY VLH on VLH.CHILD_LOCATION_FK = VRP.REGISTRATION_LOCATION_FK
        LEFT OUTER JOIN LOCATION VL ON VL.ID = VLH.PARENT_LOCATION_FK AND VL.LOCATION_LEVEL_FK = 21
        LEFT OUTER JOIN VESSEL_FEATURES VF ON VF.VESSEL_FK = T.VESSEL_FK
        AND VF.START_DATE <= T.DEPARTURE_DATE_TIME
        AND COALESCE(VF.END_DATE, to_date('01-01-2100','DD-MM-YYYY')) >= T.DEPARTURE_DATE_TIME
        INNER JOIN DEPARTMENT D on D.ID = T.RECORDER_DEPARTMENT_FK
        LEFT OUTER JOIN LOCATION_HIERARCHY DLH on DLH.CHILD_LOCATION_FK = D.LOCATION_FK
        LEFT OUTER JOIN LOCATION DL ON DL.ID = DLH.PARENT_LOCATION_FK AND DL.LOCATION_LEVEL_FK = 21
        LEFT OUTER JOIN VESSEL_USE_MEASUREMENT VUM ON VUM.TRIP_FK = T.ID
        LEFT OUTER JOIN QUALITATIVE_VALUE QV on QV.ID = VUM.QUALITATIVE_VALUE_FK
        LEFT OUTER JOIN QUALITY_FLAG ON QUALITY_FLAG.ID = T.QUALITY_FLAG_FK
WHERE
    1=1
  AND (P.LABEL IN ('LOGBOOK-LOBSTER'))
  AND (T.ID IN (297816, 297901))
  AND T.CONTROL_DATE IS NOT NULL
GROUP BY
    UPPER('S'),PL.LABEL,VL.LABEL,P.LABEL,T.ID,VF.LENGTH_OVER_ALL / 100,VF.ADMINISTRATIVE_POWER,CAST(FLOOR(COALESCE(VF.GROSS_TONNAGE_GT, VF.GROSS_TONNAGE_GRT) / 100) AS INT),4,L.LABEL,FLOOR(T.RETURN_DATE_TIME - T.DEPARTURE_DATE_TIME),T.VESSEL_FK,COALESCE(DL.LABEL, PL.LABEL),(CASE UPPER('Observer') WHEN 'SELFSAMPLING' THEN 'SelfSampling' ELSE 'Observer' END),T.DEPARTURE_DATE_TIME,T.RETURN_DATE_TIME,QUALITY_FLAG.NAME,T.COMMENTS
ORDER BY
    T.ID ASC
;

-- Create table HH

CREATE TABLE EXT_HH_425648064 NOLOGGING AS
SELECT DISTINCT
    T.SAMPLING_TYPE "SAMPLING_TYPE",
    T.LANDING_COUNTRY "LANDING_COUNTRY",
    T.VESSEL_FLAG_COUNTRY "VESSEL_FLAG_COUNTRY",
    T.YEAR "YEAR",
    T.PROJECT "PROJECT",
    T.TRIP_CODE "TRIP_CODE",
    ROW_NUMBER() OVER (PARTITION BY O.FISHING_TRIP_FK ORDER BY O.START_DATE_TIME) "STATION_NUMBER",
        MAX(CASE TO_NUMBER(O.QUALITY_FLAG_FK)
                WHEN 4 THEN UPPER('I')
                WHEN 6 THEN UPPER('I')
                WHEN 7 THEN UPPER('I')
                ELSE UPPER('V')
            END) "FISHING_VALIDITY",
    UPPER('H') "AGGREGATION_LEVEL",
    'All' "CATCH_REGISTRATION",
    'Par' "SPECIES_REGISTRATION",
    MAX(TO_CHAR(COALESCE(O.FISHING_START_DATE_TIME, O.START_DATE_TIME), 'YYYY-MM-DD')) "DATE",
    MAX(TO_CHAR(COALESCE(O.FISHING_START_DATE_TIME, O.START_DATE_TIME), 'HH24:MI')) "TIME",
    MAX(CAST(FLOOR((COALESCE(O.FISHING_END_DATE_TIME, O.END_DATE_TIME) - COALESCE(O.FISHING_START_DATE_TIME, O.START_DATE_TIME)) * 24 * 60) as INTEGER)) "FISHING_TIME",
    MAX(CAST(P_START.LATITUDE AS NUMBER(38, 7))) "POS_START_LAT",
    MAX(CAST(P_START.LONGITUDE AS NUMBER(38, 7))) "POS_START_LON",
    MAX(CAST(P_END.LATITUDE AS NUMBER(38, 7))) "POS_END_LAT",
    MAX(CAST(P_END.LONGITUDE AS NUMBER(38, 7))) "POS_END_LON",
    MAX(
            CASE WHEN L_AREA.LOCATION_LEVEL_FK IN (-1) THEN
                     L_AREA.LABEL
                 ELSE
                     CAST(null as VARCHAR2(10))
                END
    ) "AREA",
    MAX(
            CASE WHEN L_AREA.LOCATION_LEVEL_FK IN (11) THEN
                     L_AREA.LABEL
                 ELSE
                     CAST(null as VARCHAR2(40))
                END
    ) "STATISTICAL_RECTANGLE",
    CAST(null as VARCHAR2(10)) "SUB_POLYGON",
    MAX(CASE VUM.PMFM_FK WHEN -1 THEN CAST(ROUND(VUM.NUMERICAL_VALUE) AS INTEGER) ELSE NULL END) "MAIN_FISHING_DEPTH",
    MAX(CASE VUM.PMFM_FK WHEN -1 THEN CAST(ROUND(VUM.NUMERICAL_VALUE) AS INTEGER) ELSE NULL END) "MAIN_WATER_DEPTH",
    MAX(CASE TG_M.TAXON_GROUP_TYPE_FK WHEN -1 THEN M.LABEL ELSE NULL END) "NATIONAL_METIER",
    MAX(CASE TG_M.TAXON_GROUP_TYPE_FK WHEN -1 THEN M.LABEL ELSE NULL END) "EU_METIER_LEVEL5",
    CAST(null as VARCHAR2(10)) "EU_METIER_LEVEL6",
    MAX(CASE PGM.PMFM_FK WHEN -1 THEN CAST(ROUND(PGM.NUMERICAL_VALUE) AS INTEGER) ELSE NULL END) "MESH_SIZE",
    MAX(CASE WHEN PGM.PMFM_FK IN (-1) THEN PGQV.LABEL ELSE NULL END) "SELECTION_DEVICE",
    CAST(null as VARCHAR2(10)) "MESH_SIZE_IN_SELECTION_DEVICE",
    O.ID "STATION_ID",
    O.CATCH_BATCH_FK "CATCH_BATCH_ID",
    CAST(O.COMMENTS AS VARCHAR2(2000)) "STATION_COMMENTS",
    MAX(CAST(PG.COMMENTS AS VARCHAR2(2000))) "GEAR_COMMENTS",
    MAX(CASE VUM.PMFM_FK WHEN 253 THEN CAST(VUM.NUMERICAL_VALUE AS INT) ELSE NULL END) "GEAR_PHYSICAL_GEAR_NUMBER",
    MAX(CASE VUM.PMFM_FK WHEN 352 THEN QV.LABEL || ' - ' || QV.NAME ELSE NULL END) "SUBSTRATE_TYPE"
FROM
    EXT_TR_425648064 T
        INNER JOIN SIH2_ADAGIO_DBA.OPERATION O ON O.FISHING_TRIP_FK = T.TRIP_CODE
        LEFT OUTER JOIN SIH2_ADAGIO_DBA.GEAR_USE_FEATURES GUF ON GUF.OPERATION_FK = O.ID
        LEFT OUTER JOIN METIER M ON M.ID = GUF.METIER_FK
        LEFT OUTER JOIN TAXON_GROUP TG_M ON TG_M.ID = M.TAXON_GROUP_FK
        LEFT OUTER JOIN PHYSICAL_GEAR PG ON PG.ID = O.GEAR_PHYSICAL_FEATURES_FK
        LEFT OUTER JOIN GEAR G ON G.ID = PG.GEAR_FK
        LEFT OUTER JOIN VESSEL_USE_MEASUREMENT VUM ON VUM.OPERATION_FK = O.ID
        LEFT OUTER JOIN QUALITATIVE_VALUE QV on QV.ID = VUM.QUALITATIVE_VALUE_FK
        LEFT OUTER JOIN PHYSICAL_GEAR_MEASUREMENT PGM ON PGM.PHYSICAL_GEAR_FK = PG.ID
        LEFT OUTER JOIN QUALITATIVE_VALUE PGQV on PGQV.ID = PGM.QUALITATIVE_VALUE_FK
        LEFT OUTER JOIN FISHING_AREA FA ON FA.OPERATION_FK = O.ID
        LEFT OUTER JOIN LOCATION L_AREA ON L_AREA.ID = FA.LOCATION_FK
        LEFT OUTER JOIN VESSEL_POSITION P_START ON P_START.OPERATION_FK = O.ID and P_START.DATE_TIME=COALESCE(O.FISHING_START_DATE_TIME, O.START_DATE_TIME)
        LEFT OUTER JOIN VESSEL_POSITION P_END ON P_END.OPERATION_FK = O.ID and P_END.DATE_TIME=COALESCE(O.FISHING_END_DATE_TIME, O.END_DATE_TIME)
WHERE
    1=1
  AND NOT (O.START_DATE_TIME=T.DEPARTURE_DATE_TIME AND O.END_DATE_TIME=T.RETURN_DATE_TIME)
GROUP BY
    T.SAMPLING_TYPE,T.LANDING_COUNTRY,T.VESSEL_FLAG_COUNTRY,T.YEAR,T.PROJECT,T.TRIP_CODE,O.FISHING_TRIP_FK,O.START_DATE_TIME,UPPER('H'),'All','Par',O.ID,O.CATCH_BATCH_FK,CAST(O.COMMENTS AS VARCHAR2(2000))
;


-- Create RAW SL

CREATE TABLE EXT_RAW_SL_425648064 NOLOGGING AS
SELECT
    S.SAMPLING_TYPE "SAMPLING_TYPE",
    S.LANDING_COUNTRY "LANDING_COUNTRY",
    S.VESSEL_FLAG_COUNTRY "VESSEL_FLAG_COUNTRY",
    S.YEAR "YEAR",
    S.PROJECT "PROJECT",
    S.TRIP_CODE "TRIP_CODE",
    S.STATION_NUMBER "STATION_NUMBER",
    COALESCE(TN.COMMENTS, TN.NAME, TG.COMMENTS, TG.NAME) "SPECIES",
    MAX(
            CASE
                WHEN SM.PMFM_FK = -1 THEN QV.LABEL
                WHEN SPECIE_B.IS_DISCARD=1 THEN 'DIS'
                WHEN SPECIE_B.IS_LANDING=1 THEN 'LAN'
                ELSE NULL
                END
    ) "CATCH_CATEGORY",
    MAX(
            CASE
                WHEN SM.PMFM_FK = -1 THEN QV.LABEL
                WHEN SPECIE_B.IS_LANDING=1 THEN 'HUC'
                ELSE NULL
                END
    ) "LANDING_CATEGORY",
    MAX(
            CASE
                WHEN SPECIE_B.IS_LANDING=1 AND TG.LABEL = 'NEP' THEN 'Nephrops'
                WHEN SPECIE_B.IS_LANDING=1 THEN 'EU'
                ELSE NULL
                END
    ) "COMMERCIAL_SIZE_CATEGORY_SCALE",
    MAX(CASE WHEN SM.PMFM_FK IN (174,-1) THEN QV.LABEL ELSE NULL END) "COMMERCIAL_SIZE_CATEGORY",
    MAX(CASE SM.PMFM_FK WHEN 176 THEN QV.LABEL ELSE NULL END) "SUBSAMPLING_CATEGORY",
    CAST(NULL AS VARCHAR(1)) "SEX",
    MAX(SPECIE_B.ELEVATE_WEIGHT) "WEIGHT",
    MAX(CASE
            WHEN SAMPLING_B.SAMPLING_RATIO IS NULL THEN SPECIE_B.ELEVATE_WEIGHT
            WHEN SAMPLING_B.SAMPLING_RATIO > 0 THEN SPECIE_B.ELEVATE_WEIGHT * SAMPLING_B.SAMPLING_RATIO
            ELSE NULL
        END) "SUBSAMPLE_WEIGHT",
    MAX(SPECIE_B.ELEVATE_INDIVIDUAL_COUNT) "INDIVIDUAL_COUNT",
    MAX(COALESCE(SAMPLING_B.INDIVIDUAL_COUNT,SPECIE_B.INDIVIDUAL_COUNT)) "SUBSAMPLE_INDIVIDUAL_COUNT",
    UNIT.LABEL "LENGTH_CODE",
    CASE
        WHEN SAMPLING_B.ELEVATE_RTP_WEIGHT > 0 THEN
            TRUNC(CAST(SUM(LENGTH_B.ELEVATE_RTP_WEIGHT) / COUNT(distinct SM.ID) / SAMPLING_B.ELEVATE_RTP_WEIGHT + 0.0000005 as DECIMAL(8,7)), 6)
        ELSE 1
        END "SUBSAMPLING_RATIO",
    S.STATION_ID "STATION_ID",
    MAX(COALESCE(SAMPLING_B.ID, SPECIE_B.ID)) "SAMPLE_ID",
    MAX(COALESCE(SAMPLING_B.FLAT_RANK_ORDER, SPECIE_B.FLAT_RANK_ORDER)) "SAMPLE_RANK_ORDER",
    LENGTH_B.REFERENCE_TAXON_FK "REFERENCE_TAXON_ID",
    LENGTH_SV.PARAMETER_FK "PARAMETER_ID"
FROM
    EXT_HH_425648064 S
        INNER JOIN DENORMALIZED_BATCH SPECIE_B ON SPECIE_B.OPERATION_FK = S.STATION_ID
        INNER JOIN TAXON_GROUP TG ON TG.ID = SPECIE_B.INHERITED_TAXON_GROUP_FK
        LEFT OUTER JOIN DENORMALIZED_BATCH_SORT_VAL SM ON SM.BATCH_FK=SPECIE_B.ID
        LEFT OUTER JOIN QUALITATIVE_VALUE QV ON QV.ID=SM.QUALITATIVE_VALUE_FK
        LEFT OUTER JOIN DENORMALIZED_BATCH SAMPLING_B ON SAMPLING_B.PARENT_BATCH_FK = SPECIE_B.ID AND COALESCE(SAMPLING_B.SAMPLING_RATIO, 1) > 0
        LEFT OUTER JOIN DENORMALIZED_BATCH LENGTH_B ON LENGTH_B.PARENT_BATCH_FK=COALESCE(SAMPLING_B.ID, SPECIE_B.ID)
        LEFT OUTER JOIN DENORMALIZED_BATCH_SORT_VAL LENGTH_SV ON LENGTH_SV.BATCH_FK=LENGTH_B.ID AND LENGTH_SV.PMFM_FK in (-1,157) AND LENGTH_SV.IS_INHERITED=0
        LEFT OUTER JOIN UNIT ON UNIT.ID = LENGTH_SV.UNIT_FK
        LEFT OUTER JOIN TAXON_NAME TN ON TN.REFERENCE_TAXON_FK = LENGTH_B.REFERENCE_TAXON_FK AND TN.IS_REFERENT=1
WHERE
    1=1
  AND SPECIE_B.LABEL NOT LIKE 'SORTING_BATCH_INDIVIDUAL#%'
  AND (SAMPLING_B.ID IS NULL OR SAMPLING_B.LABEL NOT LIKE 'SORTING_BATCH_INDIVIDUAL#%')
  AND SPECIE_B.TAXON_GROUP_FK IS NOT NULL
  AND (SPECIE_B.WEIGHT IS NOT NULL OR SPECIE_B.ELEVATE_INDIVIDUAL_COUNT IS NOT NULL)
GROUP BY
    S.SAMPLING_TYPE,S.LANDING_COUNTRY,S.VESSEL_FLAG_COUNTRY,S.YEAR,S.PROJECT,S.TRIP_CODE,S.STATION_NUMBER,COALESCE(TN.COMMENTS, TN.NAME, TG.COMMENTS, TG.NAME),UNIT.LABEL,S.STATION_ID,LENGTH_B.REFERENCE_TAXON_FK,LENGTH_SV.PARAMETER_FK,
    SPECIE_B.ID,SAMPLING_B.ID,SAMPLING_B.ELEVATE_RTP_WEIGHT
ORDER BY
    TRIP_CODE ASC, STATION_NUMBER ASC, SAMPLE_RANK_ORDER ASC;

select * from EXT_RAW_SL_425648064;

-- Create table SL

CREATE TABLE EXT_SL_425648064 NOLOGGING AS
SELECT DISTINCT
    SL.SAMPLING_TYPE "SAMPLING_TYPE",
    SL.LANDING_COUNTRY "LANDING_COUNTRY",
    SL.VESSEL_FLAG_COUNTRY "VESSEL_FLAG_COUNTRY",
    SL.YEAR "YEAR",
    SL.PROJECT "PROJECT",
    SL.TRIP_CODE "TRIP_CODE",
    SL.STATION_NUMBER "STATION_NUMBER",
    SL.SPECIES "SPECIES",
    SL.CATCH_CATEGORY "CATCH_CATEGORY",
    (CASE SL.CATCH_CATEGORY WHEN 'LAN' THEN SL.LANDING_CATEGORY ELSE NULL END) "LANDING_CATEGORY",
    SL.COMMERCIAL_SIZE_CATEGORY_SCALE "COMMERCIAL_SIZE_CATEGORY_SCALE",
    (CASE SL.COMMERCIAL_SIZE_CATEGORY WHEN 'NA' THEN NULL ELSE SL.COMMERCIAL_SIZE_CATEGORY END) "COMMERCIAL_SIZE_CATEGORY",
    SL.SUBSAMPLING_CATEGORY "SUBSAMPLING_CATEGORY",
    SL.SEX "SEX",
    CAST(SUM(SL.WEIGHT * SL.SUBSAMPLING_RATIO * 1000) AS INTEGER) "WEIGHT",
    SUM(CASE
            WHEN SUBSAMPLE_WEIGHT > 0 THEN CAST(SL.SUBSAMPLE_WEIGHT * SL.SUBSAMPLING_RATIO * 1000 AS INTEGER)
            ELSE NULL
        END) "SUBSAMPLE_WEIGHT",
    MAX(ROUND(SL.INDIVIDUAL_COUNT * SL.SUBSAMPLING_RATIO * 100) / 100) "INDIVIDUAL_COUNT",
    MAX(ROUND(SL.SUBSAMPLE_INDIVIDUAL_COUNT * SL.SUBSAMPLING_RATIO * 100) / 100) "SUBSAMPLE_INDIVIDUAL_COUNT",
    SL.LENGTH_CODE "LENGTH_CODE",
    SL.STATION_ID "STATION_ID",
    LISTAGG(SL.SAMPLE_ID, ',') WITHIN GROUP (ORDER BY SL.SAMPLE_ID) "SAMPLE_IDS"
FROM
    EXT_RAW_SL_425648064 SL
WHERE
    1=1
GROUP BY
    SL.SAMPLING_TYPE,SL.LANDING_COUNTRY,SL.VESSEL_FLAG_COUNTRY,SL.YEAR,SL.PROJECT,SL.TRIP_CODE,SL.STATION_NUMBER,SL.SPECIES,SL.CATCH_CATEGORY,(CASE SL.CATCH_CATEGORY WHEN 'LAN' THEN SL.LANDING_CATEGORY ELSE NULL END),SL.COMMERCIAL_SIZE_CATEGORY_SCALE,(CASE SL.COMMERCIAL_SIZE_CATEGORY WHEN 'NA' THEN NULL ELSE SL.COMMERCIAL_SIZE_CATEGORY END),SL.SUBSAMPLING_CATEGORY,SL.SEX,SL.LENGTH_CODE,SL.STATION_ID
;

--CREATE TABLE EXT_HL_425648064 NOLOGGING AS
WITH BATCH_LENGTH as (
    SELECT distinct
        SL.SAMPLE_ID "SAMPLE_ID",
        B.ID "ID",
        (SELECT QV.LABEL FROM SORTING_MEASUREMENT_B SM_SEX INNER JOIN QUALITATIVE_VALUE QV ON QV.ID=SM_SEX.QUALITATIVE_VALUE_FK WHERE SM_SEX.BATCH_FK = B.ID and SM_SEX.PMFM_FK=171) "SEX",
        CAST(CASE PMFM_LENGTH.UNIT_FK WHEN 17 THEN SM_LENGTH.NUMERICAL_VALUE*10 WHEN 21 THEN SM_LENGTH.NUMERICAL_VALUE ELSE null END AS INTEGER) "LENGTH_CLASS",
        COALESCE(B.INDIVIDUAL_COUNT,1) "INDIVIDUAL_COUNT",
        B.COMMENTS "COMMENTS",
        P_LENGTH.LABEL "PARAMETER_LABEL",
        P_LENGTH.NAME "PARAMETER_NAME"
    FROM
        EXT_RAW_SL_425648064 SL
            INNER JOIN BATCH B ON B.PARENT_BATCH_FK=SL.SAMPLE_ID AND B.LABEL LIKE UPPER('SORTING_BATCH_INDIVIDUAL#%')
            INNER JOIN SORTING_MEASUREMENT_B SM_LENGTH ON SM_LENGTH.BATCH_FK = B.ID AND SM_LENGTH.PMFM_FK IN (-1,157)
            INNER JOIN PMFM PMFM_LENGTH ON PMFM_LENGTH.ID = SM_LENGTH.PMFM_FK
            INNER JOIN PARAMETER P_LENGTH ON P_LENGTH.ID = PMFM_LENGTH.PARAMETER_FK )
SELECT
    SL.SAMPLING_TYPE "SAMPLING_TYPE",
    SL.LANDING_COUNTRY "LANDING_COUNTRY",
    SL.VESSEL_FLAG_COUNTRY "VESSEL_FLAG_COUNTRY",
    SL.YEAR "YEAR",
    SL.PROJECT "PROJECT",
    SL.TRIP_CODE "TRIP_CODE",
    SL.STATION_NUMBER "STATION_NUMBER",
    SL.SPECIES "SPECIES",
    SL.CATCH_CATEGORY "CATCH_CATEGORY",
    (CASE SL.CATCH_CATEGORY WHEN 'LAN' THEN SL.LANDING_CATEGORY ELSE NULL END) "LANDING_CATEGORY",
    SL.COMMERCIAL_SIZE_CATEGORY_SCALE "COMMERCIAL_SIZE_CATEGORY_SCALE",
    (CASE SL.COMMERCIAL_SIZE_CATEGORY WHEN 'NA' THEN NULL ELSE SL.COMMERCIAL_SIZE_CATEGORY END) "COMMERCIAL_SIZE_CATEGORY",
    SL.SUBSAMPLING_CATEGORY "SUBSAMPLING_CATEGORY",
    B.SEX "SEX",
    B.LENGTH_CLASS "LENGTH_CLASS",
    SUM(B.INDIVIDUAL_COUNT) / COUNT(DISTINCT SM.ID) "NUMBER_AT_LENGTH",
    B.PARAMETER_NAME "MEASURE_TYPE",
    B.ID "MEASURE_ID",
    MAX(CASE SM.PMFM_FK WHEN 353 THEN SM.NUMERICAL_VALUE ELSE NULL END) "BIOLOGICAL_WEIGHT_G",
    MAX(CASE SM.PMFM_FK WHEN 354 THEN
                            (CASE SM.NUMERICAL_VALUE WHEN 1 THEN UPPER('Y') WHEN 0 THEN UPPER('N') END)
                        ELSE NULL END) "EGGS",
    MAX(CASE SM.PMFM_FK WHEN 355 THEN
                            (CASE SM.NUMERICAL_VALUE WHEN 1 THEN UPPER('Y') WHEN 0 THEN UPPER('N') END)
                        ELSE NULL END) "TAR_SPOT",
    MAX(CASE SM.PMFM_FK WHEN 356 THEN
                            (CASE SM.NUMERICAL_VALUE WHEN 1 THEN UPPER('Y') WHEN 0 THEN UPPER('N') END)
                        ELSE NULL END) "SETAE_HAIR",
    MAX(CASE SM.PMFM_FK WHEN 357 THEN SM.ALPHANUMERICAL_VALUE ELSE NULL END) "TAG_NO",
    SL.STATION_ID "STATION_ID",
    SL.SAMPLE_ID "SAMPLE_ID",
    REGEXP_REPLACE(
            LISTAGG(B.ID, ',') WITHIN GROUP (ORDER BY B.ID) ,'([^,]+)(,\1)*(,|$)', '\1\3'
    ) "MEASURE_IDS"
FROM
    EXT_RAW_SL_425648064 SL
    INNER JOIN BATCH_LENGTH B ON B.SAMPLE_ID=SL.SAMPLE_ID
    INNER JOIN SORTING_MEASUREMENT_B SM ON SM.BATCH_FK = B.ID
    LEFT OUTER JOIN QUALITATIVE_VALUE QV ON QV.ID = SM.QUALITATIVE_VALUE_FK
WHERE
    1=1
GROUP BY
    SL.SAMPLING_TYPE,SL.LANDING_COUNTRY,SL.VESSEL_FLAG_COUNTRY,SL.YEAR,SL.PROJECT,SL.TRIP_CODE,SL.STATION_NUMBER,SL.SPECIES,SL.CATCH_CATEGORY,(CASE SL.CATCH_CATEGORY WHEN 'LAN' THEN SL.LANDING_CATEGORY ELSE NULL END),SL.COMMERCIAL_SIZE_CATEGORY_SCALE,(CASE SL.COMMERCIAL_SIZE_CATEGORY WHEN 'NA' THEN NULL ELSE SL.COMMERCIAL_SIZE_CATEGORY END),SL.SUBSAMPLING_CATEGORY,B.SEX,B.LENGTH_CLASS,B.PARAMETER_NAME,SL.STATION_ID,SL.SAMPLE_ID,
    B.ID
;
/

/*
drop table EXT_HL_425648064;
drop table EXT_SL_425648064;
drop table EXT_RAW_SL_425648064;
drop table EXT_HH_425648064;
drop table EXT_TR_425648064;
 */