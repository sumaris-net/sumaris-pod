package net.sumaris.importation.service.rdb;

/*-
 * #%L
 * SUMARiS:: Core Importation
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import net.sumaris.core.util.Files;
import net.sumaris.importation.service.AbstractServiceTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

@Slf4j
public class RdbLoaderWriteTest extends AbstractServiceTest {

    @Autowired
    private RdbDataLoaderServiceImpl service = null;

    @Test
    public void loadTestFiles() {
        String basePath = "src/test/data/rdb/";

        // Import FRA sample file
        File file = new File(basePath, "FRA-CL-test.csv");
        loadLanding(file, "FRA");

        // Import BEL sample file
        file = new File(basePath, "BEL-CL-test.csv");
        loadLanding(file, "BEL");

        // Import GBR sample file
        file = new File(basePath, "GBR-mix-test.csv");
        loadMixed(file, "GBR");
    }

    @Test
    @Ignore
    public void loadAll() {
        String basePath = System.getProperty("user.home") + "/Documents/sumaris/data/";
        File file;

        // Import FRA file
        file = new File(basePath + "/FRA", "CL_FRA_2000-2017.csv");
        loadLanding(file, "FRA");

        // Import BEL file
        file = new File(basePath+ "/BEL", "CL_BEL-2.csv");
        loadLanding(file, "BEL");

        // Import GBR file
        file = new File(basePath+ "/GBR", "Sumaris All.txt");
        loadMixed(file, "GBR");
    }

    /* -- protected method -- */

    private void loadLanding(File file, String country) {

        try {
            service.loadLanding(file, country, true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            Files.deleteTemporaryFiles(file);
        }

    }

    private void loadMixed(File file, String country) {
        try {
            service.loadMixed(file, country, true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            Files.deleteTemporaryFiles(file);
        }
    }

    private void detectFormatAndLoad(File file, String country) {
        try {
            service.detectFormatAndLoad(file, country, true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            Files.deleteTemporaryFiles(file);
        }
    }
}
