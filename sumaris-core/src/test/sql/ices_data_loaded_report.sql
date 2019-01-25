-- ********************************************
-- Trip (CIEM format - table TR)
-- ********************************************
-- count rows by country
select
  T.VESSEL_FLAG_COUNTRY,
  count(T.ID) TR_NB_LINES
from
  FILE_ICES_TRIP T
group by T.VESSEL_FLAG_COUNTRY
order by TR_NB_LINES DESC;

-- count rows by vessel length
select
  T.VESSEL_LENGTH,
  COUNT(T.ID) TR_ROW_COUNT
from
  FILE_ICES_TRIP T
group by T.VESSEL_LENGTH
order by VESSEL_LENGTH ASC;


-- count rows by year
select
  T.YEAR,
  COUNT(T.ID) TR_ROW_COUNT
from
  FILE_ICES_TRIP T
group by YEAR
order by YEAR ASC;

-- ********************************************
-- Fishing Station (CIEM format - table HH)
-- ********************************************
-- count rows by country
select
  HH.VESSEL_FLAG_COUNTRY,
  count(HH.ID) HH_NB_LINES
from
  FILE_ICES_STATION HH
group by HH.VESSEL_FLAG_COUNTRY
order by HH_NB_LINES DESC;

-- NB station rows by metier level 5
select
  M.LABEL METIER_CODE,
  M.NAME METIER_NAME,
  count(HH.ID) HH_NB_STATION
from
  FILE_ICES_STATION HH
    inner join METIER M on M.LABEL=HH.EU_METIER_LEVEL5
group by M.LABEL, M.NAME
order by HH_NB_STATION DESC;

-- ********************************************
-- Species List (CIEM format - table SL)
-- ********************************************
-- NB rows by country
select
  SL.VESSEL_FLAG_COUNTRY,
  count(SL.ID) SL_NB_LINES
from
  FILE_ICES_SPECIES_LIST SL
group by SL.VESSEL_FLAG_COUNTRY
order by SL_NB_LINES DESC;

-- NB rows by species
select
  TG.LABEL SPECIES_LABEL,
  TG.NAME SPECIES_NAME,
  count(SL.ID) SL_NB_LINES
from
  FILE_ICES_SPECIES_LIST SL
  inner join TAXON_GROUP TG on TG.LABEL=SL.SPECIES
group by TG.LABEL, TG.NAME
order by SL_NB_LINES DESC;


-- ********************************************
-- Species Length (CIEM format - table HL)
-- ********************************************
-- rows count by country
select
  HL.VESSEL_FLAG_COUNTRY,
  count(HL.ID) HL_ROW_COUNT
from
  FILE_ICES_SPECIES_LENGTH HL
group by HL.VESSEL_FLAG_COUNTRY
order by HL_ROW_COUNT DESC;

-- rows count (and individual count) by species
select
  TG.LABEL SPECIES_LABEL,
  TG.NAME SPECIES_NAME,
  count(HL.ID) HL_ROW_COUNT,
  sum(HL.NUMBER_AT_LENGTH) INVIDUAL_COUNT
from FILE_ICES_SPECIES_LENGTH  HL
  inner join TAXON_GROUP TG on TG.LABEL=HL.SPECIES
group by TG.LABEL, TG.NAME
order by HL_ROW_COUNT DESC;