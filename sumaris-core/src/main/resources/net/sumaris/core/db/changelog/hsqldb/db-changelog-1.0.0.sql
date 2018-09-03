---
-- #%L
-- SUMARiS:: Core
-- %%
-- Copyright (C) 2018 SUMARiS Consortium
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

--  Copyright SUMARiS 2018
-- 
-- 26/02/2018 BL Creation
-- --------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- 26/02/2018 BL New table SYSTEM_VERSION, to store database schema version, after upgrade
-- -----------------------------------------------------------------------------
create table SYSTEM_VERSION (
	ID integer not null,
	LABEL VARCHAR(40) not null,
	DESCRIPTION VARCHAR(255),
	CREATION_DATE DATETIME not null,
	COMMENTS VARCHAR(2000),
	UPDATE_DATE TIMESTAMP,
	primary key (ID)
);
create sequence SYSTEM_VERSION_SEQ;

-- final commit
commit;

