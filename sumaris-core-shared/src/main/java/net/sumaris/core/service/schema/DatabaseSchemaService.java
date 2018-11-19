package net.sumaris.core.service.schema;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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




import net.sumaris.core.exception.VersionNotFoundException;
import org.nuiton.version.Version;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;

/**
 * <p>DatabaseSchemaService interface.</p>
 *
 */
@Transactional(readOnly = true)
public interface DatabaseSchemaService {

	/**
	 * Return the version, stored in the database (e.g. in SYSTEM_VERSION table)
	 *
	 * @return a {@link Version} object.
	 */
	Version getDbVersion() throws VersionNotFoundException;

	/**
	 * Return the version of the applciation. This version comes from database updates (e.g. liquibase patch)
	 *
	 * @return the version, or null if not patch available
	 */
	Version getApplicationVersion();

	/**
	 * <p>updateSchema.</p>
	 */
	@Transactional()
	void updateSchema();

    /**
     * Check if connection could be open.
     * If a validation query has been set in configuration, test it
     *
     * @return if db is loaded
     */
    boolean isDbLoaded();

    /**
     * Check if db files exists. If not a database file, return true
     *
     * @return if db files exists
     */
    boolean isDbExists();

    /**
     * Report into a file the liquibase status of database
     *
     * @param outputFile a {@link File} object.
     * @throws IOException if any.
     */
    void generateStatusReport(File outputFile) throws IOException;

    /**
     * Generate a diff report, from Hibernate model to database
     *
     * @param outputFile a {@link File} object.
     */
    void generateDiffReport(File outputFile);

    /**
     * Generate a diff changelog, from Hibernate model to database
     *
     * @param outputFile a {@link File} object.
     */
	@Transactional()
    void generateDiffChangeLog(File outputFile);

    /**
     * <p>export schema creation SQL to file</p>
     *
     * @param outputFile a {@link File} object.
     * @param withDrop a boolean.
     * @throws IOException if any.
     */
    void createSchemaToFile(File outputFile, boolean withDrop) throws IOException;

	/**
	 * <p>export schema update to file</p>
	 *
	 * @param outputFile a {@link File} object.	 *
	 * @throws IOException if any.
	 */
	void updateSchemaToFile(File outputFile) throws IOException;
}
