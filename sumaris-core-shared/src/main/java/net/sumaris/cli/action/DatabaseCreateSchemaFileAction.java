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
import net.sumaris.core.exception.SumarisTechnicalException;

import java.io.File;

/**
 * <p>DatabaseNewDbAction class.</p>
 */
@Slf4j
public class DatabaseCreateSchemaFileAction {

	public static final String COMMAND = "--schema-create-sql";
	/**
	 * <p>run.</p>
	 */
	public void run() {
		SumarisConfiguration config = SumarisConfiguration.getInstance();

		// Check output directory validity
		File outputFile = ActionUtils.checkAndGetOutputFile(false,
				this.getClass());

		DatabaseSchemaDao databaseSchemaDao = new DatabaseSchemaDaoImpl(config);

		try {
			// Create the file
			databaseSchemaDao.generateCreateSchemaFile(outputFile.getAbsolutePath());

		} catch (SumarisTechnicalException e1) {
			log.error(e1.getMessage());

            // stop here
            return;
		}
	}
}
