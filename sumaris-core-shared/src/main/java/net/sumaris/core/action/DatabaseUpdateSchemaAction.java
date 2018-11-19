package net.sumaris.core.action;

/*-
 * #%L
 * Sumaris3 Core :: Sumaris3 Core Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 Ifremer
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


import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.dao.schema.DatabaseSchemaDaoImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.exception.DatabaseSchemaUpdateException;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * <p>Update database schema class.</p>
 */
public class DatabaseUpdateSchemaAction {
	/* Logger */
	private static final Log log = LogFactory.getLog(DatabaseUpdateSchemaAction.class);

	/**
	 * <p>run.</p>
	 */
	public void run() {
		SumarisConfiguration config = SumarisConfiguration.getInstance();

		DatabaseSchemaDao databaseSchemaDao = new DatabaseSchemaDaoImpl(config);

		try {
			// Update the DB schema
			databaseSchemaDao.updateSchema();
		} catch (SumarisTechnicalException | DatabaseSchemaUpdateException e1) {
			log.error(e1.getMessage());

            // stop here
            return;
		}
	}
}
