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
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.service.ServiceLocator;
import net.sumaris.core.service.technical.schema.DatabaseSchemaService;
import org.nuiton.version.Version;

/**
 * <p>Update database schema class.</p>
 */
@Slf4j
public class DatabaseUpdateSchemaAction {

	/**
	 * <p>run.</p>
	 */
	public void run() {
		DatabaseSchemaService service = ServiceLocator.instance().getDatabaseSchemaService();

		log.info("Starting schema update...");

		// Check if database is well loaded
		if (!service.isDbLoaded()) {
			log.warn("Could not update the schema: database seems to be empty!");
			return;
		}

		// Getting the database version
		try {
			Version actualDbVersion = service.getSchemaVersion().orElse(null);
			// result could be null, is DB is empty (mantis #21013)
			if (actualDbVersion == null) {
				log.warn("Could not find database schema version");
			}
			else {
				log.info(String.format("Database schema version is [%s]", actualDbVersion));
			}

		} catch (SumarisTechnicalException e) {
			log.error("Error while getting database version.", e);
		}

		// Getting the version after update
		try {
			Version expectedDbVersion = service.getApplicationVersion();
			if (expectedDbVersion == null) {
				log.warn("Unable to find the database schema version AFTER the update. Nothing to update !");
				return;
			}
			log.info(String.format("Database schema version AFTER the update should be [%s]", expectedDbVersion));
		} catch (SumarisTechnicalException e) {
			log.error("Error while getting database version AFTER the update.", e);
		}

		// Run the update process
		try {
			log.info("Launching update...");
			service.updateSchema();
			log.info("Database schema successfully updated.");
		} catch (SumarisTechnicalException e) {
			log.error("Error while updating the database schema.", e);
		}
	}
}
