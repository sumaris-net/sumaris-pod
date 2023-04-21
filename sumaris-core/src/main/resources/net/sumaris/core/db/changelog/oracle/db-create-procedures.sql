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
--  16/05/19 BL Creation (add F_TO_RECTANGLE)
--


-- --------------------------------------------------------------------------
--
-- SPLIT function - useful to generate 'create synonym' order
--   -> see file 'create-other-users-synonyms.sql' for more details
--
-- --------------------------------------------------------------------------
CREATE OR REPLACE type split_tbl as table of varchar2(32767);
//

CREATE OR REPLACE FUNCTION "SPLIT"
(
    p_list varchar2,
    p_del varchar2 := ',',
    p_nb_limit INTEGER := -1
) return split_tbl pipelined
is
--$ ********************************************************************
--$
--$  MOD : SPLIT
--$  ROL : Split value, to be used in a join query.
--$
--$  param :
--$    - p_list: List of value
--$    - p_del: the delimiter
--$    - p_nb_limit: maximum number of values to split, or -1 for 'no limit'
--$
--$  return : the rectangle label
--$
--$  example : The query: 'SELECT T.COLUMN_VALUE from table(split ('VALUE1,VALUE2', ',')) T'
--$        will return:
--$          /--------------\
--$          | COLUMN_VALUE |
--$          |--------------|
--$          |    VALUE1    |
--$          |    VALUE2    |
--$          \--------------/
--$
--$ History :
--$  16/05/19 BL Creation (used by extraction - e.g. ICES RDB and COST formats)
--$
--$ ********************************************************************
    l_idx     pls_integer;
    l_counter pls_integer;
    l_list    varchar2(32767) := p_list;
begin
    l_counter := 0;
    loop
        l_counter := l_counter + 1;
        l_idx := instr(l_list,p_del);
        if l_idx > 0 and (p_nb_limit = -1 or l_counter < p_nb_limit) then
            pipe row(substr(l_list,1,l_idx-1));
            l_list := substr(l_list,l_idx+length(p_del));
        else
            pipe row(l_list);
            exit;
        end if;
    end loop;
    return;
end SPLIT;
//

CREATE OR REPLACE FUNCTION F_TO_RECTANGLE
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

-- --------------------------------------------------------------------------
--
-- Function to fill TAXON_GROUP_HIERARCHY and TAXON_GROUP2TAXON_HIERARCHY tables
--
-- --------------------------------------------------------------------------
-- 10/02/16 EB  Trace execution in PROCESSING_HISTORY
-- --------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE P_FILL_TAXON_GROUP_HIERARCHY
AS
    PATH_FATHER VARCHAR2(255);
    NB_FATHERS NUMBER;
    FATHER_ID NUMBER;
    CURRENT_FATHER NUMBER;
    CURRENT_PROCESSING_FK NUMBER;

CURSOR c IS (
                  SELECT
                    CHILD_ID,
                    (LEVEL - 1) as NB_FATHERS,
                    substr(SYS_CONNECT_BY_PATH(FATHER_ID,'-'),2) as PATH_FATHER
                  FROM (
                    SELECT
                      ID as CHILD_ID,
                      PARENT_TAXON_GROUP_FK as FATHER_ID
                    FROM
                      TAXON_GROUP
                  )
                  -- WHERE LEVEL > 1
                  START WITH FATHER_ID IS NULL
                  CONNECT BY PRIOR CHILD_ID = FATHER_ID
              );
CURSOR c2 IS (
                  SELECT DISTINCT
                    TG.ID as CHILD_ID,
                    (LEVEL - 1) as NB_FATHERS,
                    substr(SYS_CONNECT_BY_PATH(TG.PARENT_TAXON_GROUP_FK,'-'),2) as PATH_FATHER,
                    TGHR.REFERENCE_TAXON_FK,
                    TGHR.START_DATE as START_DATE,
                    TGHR.END_DATE as END_DATE
                  FROM
                    TAXON_GROUP TG
                    left outer join TAXON_GROUP_HISTORICAL_RECORD TGHR
                        on TG.ID = TGHR.TAXON_GROUP_FK
                  WHERE LEVEL > 1
                  START WITH TG.PARENT_TAXON_GROUP_FK IS NULL
                  CONNECT BY PRIOR TG.ID = TG.PARENT_TAXON_GROUP_FK

              );
BEGIN
  -- trace process in processing_hisory
    insert into PROCESSING_HISTORY(ID, PROCESSING_TYPE_FK, NAME, PROCESSING_DATE,PROCESSING_STATUS_FK)
    values (PROCESSING_HISTORY_SEQ.nextval,
            (SELECT ID FROM PROCESSING_TYPE WHERE LABEL='SYS_P_FILL_TAXON_GROUP_HIERARCHY'),
            'P_FILL_TAXON_GROUP_HIERARCHY',
            systimestamp,
            (SELECT ID FROM PROCESSING_STATUS WHERE LABEL='WAITING_EXECUTION'));

    select PROCESSING_HISTORY_SEQ.currval into CURRENT_PROCESSING_FK from dual;

    ------------------------------------------------------------------------------
    -- PART 1 : fill TAXON_GROUP_HIERARCHY
    ------------------------------------------------------------------------------
    DELETE FROM TAXON_GROUP_HIERARCHY;

    -- find all couples (child/father) with the calculated ratio
    FOR i IN c LOOP

        -- find cursor data
        PATH_FATHER := i.PATH_FATHER;
    NB_FATHERS  := i.NB_FATHERS;

        -- For each parent
    FOR j IN REVERSE 1..NB_FATHERS LOOP

          -- find the current father_id and the current ratio
          CURRENT_FATHER := INSTR(PATH_FATHER, '-', 1, j);
    IF (j = NB_FATHERS) THEN
            FATHER_ID := TO_NUMBER(SUBSTR(PATH_FATHER,CURRENT_FATHER + 1));
    ELSE
            FATHER_ID := TO_NUMBER(SUBSTR(PATH_FATHER,CURRENT_FATHER + 1, INSTR(PATH_FATHER,'-',1, j+1)-CURRENT_FATHER-1));
    END IF;

          -- insert into temporay table
    INSERT INTO TAXON_GROUP_HIERARCHY (CHILD_TAXON_GROUP_FK, PARENT_TAXON_GROUP_FK)
    VALUES (i.CHILD_ID, FATHER_ID);
    END LOOP;
    END LOOP;

      -- insert link to itself
    INSERT INTO TAXON_GROUP_HIERARCHY (CHILD_TAXON_GROUP_FK, PARENT_TAXON_GROUP_FK)
    SELECT ID, ID FROM TAXON_GROUP;

    ------------------------------------------------------------------------------
    -- PART 2 : fill TAXON_GROUP2TAXON_HIERARCHY
    ------------------------------------------------------------------------------
    DELETE FROM TAXON_GROUP2TAXON_HIERARCHY;

    -- find all couples (child/father) with the calculated ratio
    FOR i IN c2 LOOP

        -- find cursor data
        PATH_FATHER := i.PATH_FATHER;
    NB_FATHERS  := i.NB_FATHERS;

    IF (i.REFERENCE_TAXON_FK is not NULL) THEN

    -- insert link between child<-->reference_taxon
    INSERT INTO TAXON_GROUP2TAXON_HIERARCHY (PARENT_TAXON_GROUP_FK, CHILD_REFERENCE_TAXON_FK, START_DATE, END_DATE, IS_INHERITED)
    SELECT i.CHILD_ID, i.REFERENCE_TAXON_FK, i.START_DATE, i.END_DATE, '0'
    FROM DUAL
    where not exists (
            select * from TAXON_GROUP2TAXON_HIERARCHY where
                    PARENT_TAXON_GROUP_FK = i.CHILD_ID
                                                        and CHILD_REFERENCE_TAXON_FK = i.REFERENCE_TAXON_FK
                                                        and START_DATE = i.START_DATE
        );

    -- For each parent
    FOR j IN REVERSE 1..NB_FATHERS LOOP

            -- find the current father_id and the current ratio
            CURRENT_FATHER := INSTR(PATH_FATHER, '-', 1, j);
    IF (j = NB_FATHERS) THEN
              FATHER_ID := TO_NUMBER(SUBSTR(PATH_FATHER,CURRENT_FATHER + 1));
    ELSE
              FATHER_ID := TO_NUMBER(SUBSTR(PATH_FATHER,CURRENT_FATHER + 1, INSTR(PATH_FATHER,'-',1, j+1)-CURRENT_FATHER-1));
    END IF;

            -- insert link between father<-->reference_taxon
    INSERT INTO TAXON_GROUP2TAXON_HIERARCHY (PARENT_TAXON_GROUP_FK, CHILD_REFERENCE_TAXON_FK, START_DATE, END_DATE, IS_INHERITED)
    SELECT FATHER_ID, i.REFERENCE_TAXON_FK, i.START_DATE, i.END_DATE, '1'
    FROM DUAL
    where not exists (
            select * from TAXON_GROUP2TAXON_HIERARCHY where
                    PARENT_TAXON_GROUP_FK = FATHER_ID
                                                        and CHILD_REFERENCE_TAXON_FK = i.REFERENCE_TAXON_FK
                                                        and START_DATE = i.START_DATE
        );
    END LOOP;
    END IF;
    END LOOP;

      --  update processing_history status
    UPDATE PROCESSING_HISTORY
    SET PROCESSING_STATUS_FK = 'SUCCESS'
    WHERE ID = CURRENT_PROCESSING_FK;

    -- commit changes
    COMMIT;

    EXCEPTION
        WHEN OTHERS THEN
          --  update processing_history status
    UPDATE PROCESSING_HISTORY
    SET PROCESSING_STATUS_FK = 'ERROR'
    WHERE ID = CURRENT_PROCESSING_FK;

    raise_application_error(-20001,'An error was encountered - '||SQLCODE||' -ERROR- '||SQLERRM);
END;
//
