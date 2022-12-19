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


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.event.schema.SchemaEvent;
import net.sumaris.core.event.schema.SchemaReadyEvent;
import net.sumaris.core.event.schema.SchemaUpdatedEvent;
import net.sumaris.core.exception.DatabaseSchemaUpdateException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.exception.VersionNotFoundException;
import org.nuiton.version.Version;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * <p>DatabaseSchemaServiceImpl class.</p>
 *
 * @author Lionel Touseau <lionel.touseau@e-is.pro>
 */
@Service("databaseSchemaService")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaServiceImpl implements DatabaseSchemaService {

    private boolean isApplicationReady;

	protected final SumarisConfiguration config;

    protected final DatabaseSchemaDao databaseSchemaDao;

    private final ApplicationEventPublisher publisher;

    protected final Optional<TaskExecutor> taskExecutor;

    @PostConstruct
    protected void init() {

        // Run schema update, if need
        boolean enableLiquibase = config.isLiquibaseEnabled();
        if (enableLiquibase) {
            // Do the update (but do NOT emit event)
            try {
                databaseSchemaDao.updateSchema();
            } catch (DatabaseSchemaUpdateException e) {
                throw new SumarisTechnicalException(e.getCause());
            }
        }
        else if (log.isDebugEnabled()){
            log.debug( String.format("Liquibase did not run because configuration option '%s' set to false.",
                            SumarisConfigurationOption.LIQUIBASE_ENABLED.getKey()));
        }

        // Publish ready event
        publishSchemaReadyEvent();
    }

    @EventListener
    protected void onApplicationReady(ApplicationStartedEvent event) {
        this.isApplicationReady = true;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Version> getSchemaVersion() {
        try {
            if (!isDbLoaded()) {
                throw new VersionNotFoundException("Unable to find Database version: database is empty");
            }
            return Optional.of(databaseSchemaDao.getSchemaVersion());
        } catch (VersionNotFoundException e) {
            if (log.isWarnEnabled()) {
                log.warn(e.getMessage());
            }
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Version getApplicationVersion() {
        return  databaseSchemaDao.getSchemaVersionIfUpdate();
    }

    /** {@inheritDoc} */
    @Override
    public void updateSchema() {
        try {
            databaseSchemaDao.updateSchema();
        } catch (DatabaseSchemaUpdateException e) {
            throw new SumarisTechnicalException(e.getCause());
        }

        // Emit events
        publishSchemaUpdatedEvent();

    }

    /** {@inheritDoc} */
    @Override
    public boolean isDbLoaded() {
        return databaseSchemaDao.isDbLoaded();
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isDbExists() {    	
        return databaseSchemaDao.isDbExists();
    }
    
    /** {@inheritDoc} */
    @Override
    public void generateStatusReport(File outputFile) throws IOException {
        if (outputFile == null || !outputFile.getParentFile().isDirectory() || !outputFile.canWrite()) {
            log.error("Could not write into the output file. Please make sure the given path is a valid path.");
            return;
        }

        databaseSchemaDao.generateStatusReport(outputFile);
    }
    
    /** {@inheritDoc} */
    @Override
    public void generateDiffReport(File outputFile) {
        if (outputFile == null || !outputFile.getParentFile().isDirectory() || !outputFile.canWrite()) {
            log.error("Could not write into the output file. Please make sure the given path is a valid path.");
            return;
        }        
        databaseSchemaDao.generateDiffReport(outputFile, config.getLiquibaseDiffTypes());
    }
    
    /** {@inheritDoc} */
    @Override
    public void generateDiffChangeLog(File outputFile) {
        if (outputFile == null 
                || !outputFile.getParentFile().isDirectory() 
                || (outputFile.exists() && !outputFile.canWrite())) {
            log.error("Could not write into the output file. Please make sure the given path is a valid path.");
            throw new SumarisTechnicalException("Invalid path: " + outputFile.getName());
        }        
        databaseSchemaDao.generateDiffChangeLog(outputFile, config.getLiquibaseDiffTypes());
    }
    
    /** {@inheritDoc} */
    @Override
    public void createSchemaToFile(File outputFile, boolean withDrop) throws IOException {
        databaseSchemaDao.generateCreateSchemaFile(outputFile.getCanonicalPath(), false, withDrop, true);
    }

    /** {@inheritDoc} */
    @Override
    public void updateSchemaToFile(File outputFile) throws IOException {
        databaseSchemaDao.generateUpdateSchemaFile(outputFile.getCanonicalPath());
    }

    /* -- protected methods -- */

    protected void publishSchemaUpdatedEvent() {
        // Get schema version
        Version dbVersion;
        try {
            dbVersion = databaseSchemaDao.getSchemaVersion();
        } catch (VersionNotFoundException e) {
            throw new SumarisTechnicalException("Missing version after a schema update", e);
        }

        // send event
        publishEventAfterApplicationReady(new SchemaUpdatedEvent(dbVersion, config.getConnectionProperties()));
    }

    protected void publishSchemaReadyEvent() {

        // Get schema version
        Version dbVersion;
        try {
            dbVersion = databaseSchemaDao.getSchemaVersion();
        } catch (VersionNotFoundException e) {
            dbVersion = null;
        }

        // send event
        publishEventAfterApplicationReady(new SchemaReadyEvent(dbVersion, config.getConnectionProperties()));
    }

    /**
     * Publish and event, when application is ready
     * @param event
     */
    protected void publishEventAfterApplicationReady(SchemaEvent event) {
        if (!isApplicationReady && taskExecutor.isPresent()) {
            taskExecutor.get().execute(() -> {
                try {
                    while (!isApplicationReady) {
                        Thread.sleep(200); // Wait ready
                    }

                    // Emit update event
                    publisher.publishEvent(event);
                } catch (InterruptedException e) {
                }

            });
        }
        else {
            // Emit update event
            publisher.publishEvent(event);
        }
    }

}
