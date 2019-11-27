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
-- *****************************************************************
--
-- Insertion des valeurs qualitatives (pour les coef de conversion)
--
-- *****************************************************************
-- Requête ayant permis la generation des INSERT, à executer sur le schéma SIH2_ADAGIO_DBA:
--
-- ( select distinct 'insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) ',
--                     'select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, ',
--                     '''',
--                     QV.NAME,
--                     ''', ',
--                     '''',
--                     QV.DESCRIPTION,
--                     ''', ',
--                     ' P.ID FROM PARAMETER P where label=''DRESSING'' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label=''',
--                     QV.NAME,
--                     ''');'
--     from ROUND_WEIGHT_CONVERSION R
--              inner join QUALITATIVE_VALUE QV on QV.ID = R.DRESSING_fk
-- ) union (
--     select distinct
--         'insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) ',
--         'select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, ',
--         '''', QV.NAME, ''', ',
--         '''', QV.DESCRIPTION, ''', ',
--         ' P.ID FROM PARAMETER P where label=''PRESERVATION'' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label=''',
--         QV.NAME, ''');'
--     from ROUND_WEIGHT_CONVERSION R
--              inner join QUALITATIVE_VALUE QV on QV.ID = R.PRESERVING_FK
-- );
--

insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'WHL', 'Entier',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='WHL');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'FSB', 'En filets, avec peau+arêtes',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='FSB');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'GTF', 'Eviscéré, équeuté et sans nageoires',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='GTF');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'HET', 'Etêté, équeuté',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='HET');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'ROE', 'Laitance, œufs',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='ROE');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'TAL', 'Queue',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='TAL');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'FSP', 'En filets, dépouillé, avec arête intramusculaire',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='FSP');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'GTA', 'Eviscéré et équeuté',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='GTA');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'GUT', 'Eviscéré',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='GUT');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'CLA', 'Pinces',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='CLA');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'FIL', 'En filets',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='FIL');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'GHT', 'Eviscéré, étêté et équeuté',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='GHT');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'GUH', 'Eviscéré/étêté',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='GUH');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'JAT', 'Découpe japonaise et équeuté',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='JAT');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'TLD', 'Equeuté',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='TLD');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'TUB', 'Corps cylindrique uniquement',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='TUB');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'CBF', 'Double filet de cabillaud avec peau (escalado)',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='CBF');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'LAP', 'Lappen',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='LAP');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'SAL', 'Légèrement salé en saumure',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='SAL');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'SKI', 'Dépouillé',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='SKI');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'HEO', 'Tête',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='HEO');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'HEA', 'Etêté',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='HEA');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'JAP', 'Découpe japonaise',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='JAP');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'SAD', 'Salé à sec',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='SAD');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'DWT', 'Code de la CICTA',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='DWT');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'FIS', 'En filets+ dépouillé',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='FIS');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'GUL', 'Eviscéré, avec foie',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='GUL');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'SGT', 'Salé et éviscéré',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='SGT');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'WNG', 'Ailerons',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='WNG');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'CLO', 'Epatté',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='CLO');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'GUG', 'Eviscéré et sans branchies',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='GUG');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'GUS', 'Eviscéré, étêté, dépouillé',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='GUS');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'OTH', 'Autre',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='OTH');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'SGH', 'Salé, éviscéré et étêté',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='SGH');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'SUR', 'Surimi',  P.ID FROM PARAMETER P where label='DRESSING' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='SUR');

insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'SAL', 'Salé',  P.ID FROM PARAMETER P where label='PRESERVATION' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='SAL');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'FRE', 'Frais',  P.ID FROM PARAMETER P where label='PRESERVATION' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='FRE');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'ALI', 'Vivant',  P.ID FROM PARAMETER P where label='PRESERVATION' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='ALI');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'FRO', 'Congelé',  P.ID FROM PARAMETER P where label='PRESERVATION' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='FRO');
insert into QUALITATIVE_VALUE (id, status_fk, creation_date, update_date, label, name, parameter_fk) select NEXT VALUE FOR QUALITATIVE_VALUE_SEQ, 1, sysdate, now, 'DRI', 'Séché',  P.ID FROM PARAMETER P where label='PRESERVATION' and not exists (select * from QUALITATIVE_VALUE QV where QV.parameter_fk=P.ID and QV.label='DRI');
