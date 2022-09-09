package net.sumaris.core.service.technical.schema;

/*-
 * #%L
 * SUMARiS:: Core
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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.schema.DatabaseSchemaService;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.nuiton.version.Version;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

@Slf4j
public class DatabaseSchemaServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private DatabaseSchemaService service;

    @Test
    public void updateSchema() {
        service.updateSchema();

        Version dbVersion = service.getSchemaVersion().orElse(null);
        Assert.assertNotNull(dbVersion);
        log.debug("DB version is now: {}", dbVersion);
    }

    @Test
    public void createSchemaToFile() throws IOException{
        File outputFile = new File(dbResource.getResourceDirectory(), "create-schema.sql");
        service.createSchemaToFile(outputFile, false);

        String sql = FileUtils.readFileToString(outputFile, "UTF-8");
        log.debug("Generated schema create script:\n" + sql);
    }

    @Test
    public void updateSchemaToFile() throws IOException{
        File outputFile = new File(dbResource.getResourceDirectory(), "update-schema.sql");
        service.updateSchemaToFile(outputFile);

        Assert.assertTrue(outputFile.exists());
        String sql = FileUtils.readFileToString(outputFile, "UTF-8");
        log.debug("Generated schema update script:\n" + sql);
    }

    @Test
    public void generateDiffChangeLog() throws IOException{
        File outputFile = new File(dbResource.getResourceDirectory(), "db-changelog.xml");
        service.generateDiffChangeLog(outputFile);

        String diffXml = FileUtils.readFileToString(outputFile, "UTF-8");
        log.debug("Generated XML changelog:\n" + diffXml);
    }
}
