/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.importation.service.vessel;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.Files;
import net.sumaris.importation.DatabaseResource;
import net.sumaris.importation.service.AbstractServiceTest;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

@Slf4j
public class SiopVesselLoaderWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private SiopVesselLoaderService service = null;

    @Test
    public void loadFromFile() {
        String basePath = "src/test/data/vessel/";

        // Import vessel file
        File file = new File(basePath, "SIOP-vessels.csv");

        try {
            service.loadFromFile(file, "SIOP", true, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            Files.deleteTemporaryFiles(file);
        }

    }

}
