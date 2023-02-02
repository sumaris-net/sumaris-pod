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
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.util.Files;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import net.sumaris.importation.DatabaseResource;
import net.sumaris.importation.core.service.vessel.SiopVesselImportService;
import net.sumaris.importation.core.service.vessel.vo.SiopVesselImportContextVO;
import net.sumaris.importation.service.AbstractServiceTest;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

@Slf4j
public class SiopVesselLoaderWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private SiopVesselImportService service = null;

    @Autowired
    private PersonService personService = null;

    @Test
    public void assertLoadFromFile() {
        String basePath = "src/test/data/vessel/";
        File file = new File(basePath, "vessels-siop.csv");
        assertLoadFromFile(file);
    }

    @Test
    public void loadFromFileWithBOM() {
        String basePath = "src/test/data/vessel/";
        File file = new File(basePath, "vessels-siop-bom.csv");

        assertLoadFromFile(file);
    }

    @Test
    @Ignore
    public void loadFromProductionFile() {
        String basePath = System.getProperty("user.home") + "/Documents/adap/data/vessels";
        File file = new File(basePath, "bateaux_09_11_2022.csv");

        assertLoadFromFile(file);
    }

    /* -- internal -- */

    private void assertLoadFromFile(File file) {
        Assume.assumeTrue("Missing file at " + file.getAbsolutePath(), file.exists() && file.isFile());
        int userId = getAdminUserId();

        // Import vessel file
        try {
            SiopVesselImportContextVO context = SiopVesselImportContextVO.builder()
                .recorderPersonId(userId)
                .processingFile(file)
                .build();
            service.importFromFile(context, null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        finally {
            Files.deleteTemporaryFiles(file);
        }
    }

    private int getAdminUserId() {
        return personService.findByFilter(PersonFilterVO.builder()
                .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
                .userProfileId(UserProfileEnum.ADMIN.getId())
                .build(), Pageables.create(0,1))
            .stream().findFirst().map(PersonVO::getId)
            .orElseThrow(() -> new SumarisTechnicalException("No admin user found in DB"));
    }
}
