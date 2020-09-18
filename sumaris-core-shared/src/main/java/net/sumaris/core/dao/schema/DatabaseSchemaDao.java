package net.sumaris.core.dao.schema;

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

import net.sumaris.core.exception.DatabaseSchemaUpdateException;
import net.sumaris.core.exception.VersionNotFoundException;
import org.nuiton.version.Version;

import java.io.File;
import java.io.IOException;
import java.util.Properties;


/**
 * <p>DatabaseSchemaDao interface.</p>
 */
public interface DatabaseSchemaDao {

    /**
     * Generate a file with all SQL for database creation
     *
     * @param filename full path of the file to generate
     */
    void generateCreateSchemaFile(String filename);

    /**
     * <p>generateCreateSchemaFile.</p>
     *
     * @param filename   The file to generate, or null if only execution is need
     * @param doExecute  The SQL script must be execute on database ?
     * @param withDrop   generate drop statement ?
     * @param withCreate generate create statement ?
     */
    void generateCreateSchemaFile(String filename, boolean doExecute, boolean withDrop, boolean withCreate);

    /**
     * Generate a file with update SQL statement.
     * SQL statements will NOT be executed on database
     *
     * @param filename a {@link String} object.
     */
    void generateUpdateSchemaFile(String filename);

    /**
     * Generate a file with update SQL statement, and/or execute it on database.
     *
     * @param filename The file to generate, or null if only execution on database is need
     * @param doUpdate true if execution is need on database
     */
    void generateUpdateSchemaFile(String filename, boolean doUpdate);

    /**
     * Execute all changes need on database schema
     *
     * @throws DatabaseSchemaUpdateException if could not update schema
     * @since 1.0
     */
    void updateSchema() throws DatabaseSchemaUpdateException;


    /**
     * Execute all changes need on database schema, from the given connection
     *
     * @param connectionProperties the connection properties. If null, will use default (see config.getConnectionProperties())
     * @throws DatabaseSchemaUpdateException if could not update schema
     * @since 1.0
     */
    void updateSchema(Properties connectionProperties) throws DatabaseSchemaUpdateException;

    /**
     * Execute all changes need on database schema, from the given DB directory
     *
     * @param dbDirectory the DB directory. If null, will use default (see config.getConnectionProperties())
     * @throws DatabaseSchemaUpdateException if could not update schema
     * @since 1.0
     */
    void updateSchema(File dbDirectory) throws DatabaseSchemaUpdateException;

    /**
     * Retrieve the schema version, from table SYSTEM_VERSION,
     *
     * @return The database version (i.e. '3.2.3' @see Version)
     * @throws VersionNotFoundException if the version could not be found, or has a bad format
     * @since 1.0
     */
    Version getSchemaVersion() throws VersionNotFoundException;

    /**
     * Get the database schema version if updates is apply
     * (the version that the database should have if updateSchema() was called)
     *
     * @return The database version (i.e. '3.2.3' @see Version)
     * @since 1.0
     */
    Version getSchemaVersionIfUpdate();

    /**
     * Check if a update schema if need
     * This is equivalent to : <code>getSchemaVersion().compareTo(getSchemaVersionIfUpdate()) >= 0</code>
     *
     * @return true if a update is need
     * @since 1.0
     * @throws VersionNotFoundException if any.
     */
    boolean shouldUpdateSchema() throws VersionNotFoundException;
    
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
     * Generate a diff of database
     *
     * @param typesToControl a comma separated database object to check (i.e Table, View, Column...). If null, all types are checked
     * @param outputFile a {@link File} object.
     */
    void generateDiffReport(File outputFile, String typesToControl);
    
    /**
     * Generate a diff change log
     *
     * @param typesToControl a comma separated database object to check (i.e Table, View, Column...). If null, all types are checked
     * @param outputChangelogFile a {@link File} object.
     */
    void generateDiffChangeLog(File outputChangelogFile, String typesToControl);
    
    /**
     * Generate a new DB directory
     *
     * @param dbDirectory a {@link File} object.
     * @param replaceIfExists a boolean.
     */
    void generateNewDb(File dbDirectory, boolean replaceIfExists);

    /**
     * Generate a new DB directory with the specified script and connection properties
     *
     * @param dbDirectory a {@link File} object.
     * @param replaceIfExists a boolean.
     * @param scriptFile a file with the HSQLDB script (e.g. sumaris.script), or null for default script file
     * @param connectionProperties a {@link Properties} object.
     * @param isTemporaryDb is target DB is temporay DB (for synchro), some changes are done (e.g. TEMP_QUERY_PARAMETER table)
     */
    void generateNewDb(File dbDirectory, boolean replaceIfExists, File scriptFile, Properties connectionProperties, boolean isTemporaryDb);


}
