package net.sumaris.cli.action;

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


import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.dao.schema.DatabaseSchemaDaoImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.exception.DatabaseSchemaUpdateException;
import net.sumaris.core.exception.SumarisTechnicalException;

import java.io.File;

/**
 * <p>DatabaseNewDbAction class.</p>
 */
@Slf4j
public class DatabaseCreateSchemaAction {

	/**
	 * <p>run.</p>
	 */
	public void run() {
		SumarisConfiguration config = SumarisConfiguration.getInstance();

		// Check output directory validity
		File outputDirectory = ActionUtils.checkAndGetOutputFile(true,
				this.getClass());

		DatabaseSchemaDao databaseSchemaDao = new DatabaseSchemaDaoImpl(config);

		try {
			// Create the database
			databaseSchemaDao.generateNewDb(outputDirectory, false);

			// Update the DB schema
			databaseSchemaDao.updateSchema(outputDirectory);
		} catch (SumarisTechnicalException | DatabaseSchemaUpdateException e1) {
			log.error(e1.getMessage());

            // stop here
            return;
		}

        // Set this new db directory (and JDBC URL) as default database
		// This allow to chain action. e.g. --new-db --import-ref
		config.setDbDirectory(outputDirectory);
        config.setJdbcUrl(Daos.getJdbcUrl(outputDirectory, config.getDbName()));
	}
}
