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


import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.exception.DatabaseSchemaUpdateException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.exception.VersionNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.version.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * <p>DatabaseSchemaServiceImpl class.</p>
 *
 * @author Lionel Touseau <lionel.touseau@e-is.pro>
 */
@Service("databaseSchemaService")
public class DatabaseSchemaServiceImpl implements DatabaseSchemaService {

    /** Logger. */
    private static final Log log =
            LogFactory.getLog(DatabaseSchemaServiceImpl.class);
    
    @Autowired
	protected SumarisConfiguration config;

    @Autowired
    protected DatabaseSchemaDao databaseSchemaDao;
    
    /** {@inheritDoc} */
    @Override
    public Version getDbVersion() {
        Version version;
        try {
            if (!isDbLoaded()) {
                throw new VersionNotFoundException("db is not open");
            }
            version = databaseSchemaDao.getSchemaVersion();
        } catch (VersionNotFoundException e) {
            if (log.isWarnEnabled()) {
                log.warn(e.getMessage());
            }
            version = null;
        }
        return version;
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
}
