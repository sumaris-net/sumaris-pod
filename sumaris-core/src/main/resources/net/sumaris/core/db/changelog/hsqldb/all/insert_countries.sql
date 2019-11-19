-- *****************************************************************
--
-- Insertion des pays manquants (pour les coef de conversion)
--
-- *****************************************************************
-- Requête ayant permis la generation des INSERT, à executer sur le schéma SIH2_ADAGIO_DBA:
--
-- select distinct
--     'insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) ',
--      'select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, ''',
--      L.LABEL, ''', ''', L.NAME, ''' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL=''', L.LABEL, ''');'
-- from
--   ROUND_WEIGHT_CONVERSION R
--   inner join LOCATION L on L.ID = R.LOCATION_FK
-- ;

insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'CYP', 'Chypre' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='CYP');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'ESP', 'Espagne' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='ESP');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'LTU', 'Lituanie' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='LTU');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'EST', 'Estonie' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='EST');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'FRA', 'France' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='FRA');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'GRC', 'Grèce' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='GRC');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'POL', 'Pologne' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='POL');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'DEU', 'Allemagne' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='DEU');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'PRT', 'Portugal' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='PRT');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'MLT', 'Malte' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='MLT');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'FIN', 'Finlande' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='FIN');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'LVA', 'Lettonie' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='LVA');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'GBR', 'Royaume-Uni' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='GBR');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'SWE', 'Suède' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='SWE');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'NLD', 'Pays-Bas' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='NLD');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'ITA', 'Italie' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='ITA');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'BEL', 'Belgique' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='BEL');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'DNK', 'Danemark' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='DNK');
insert into LOCATION (id, status_fk, validity_status_fk, creation_date, update_date, location_level_fk, label, name) select NEXT VALUE FOR LOCATION_SEQ, 1, 1, sysdate, sysdate, 1, 'IRL', 'Irlande' from STATUS where ID=1 and NOT EXISTS (SELECT ID FROM LOCATION WHERE LOCATION_LEVEL_FK=1 AND LABEL='IRL');
