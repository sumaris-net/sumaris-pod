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

package net.sumaris.importation.service.activitycalendar;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.util.Files;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import net.sumaris.importation.DatabaseResource;
import net.sumaris.importation.core.service.activitycalendar.ListActivityCalendarImportService;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityCalendarImportResultVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityImportCalendarContextVO;
import net.sumaris.importation.core.service.vessel.SiopVesselImportService;
import net.sumaris.importation.core.service.vessel.vo.SiopVesselImportContextVO;
import net.sumaris.importation.service.AbstractServiceTest;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@Slf4j
public class ListActivityCalendarLoaderWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private SiopVesselImportService service = null;

    @Autowired
    private PersonService personService = null;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ListActivityCalendarImportService listActivityCalendarImportService;

    @Before
    public void setup() {
        // force apply software configuration
        configurationService.applySoftwareProperties();
    }

    @Test
    public void loadFromFile() {
        String basePath = "src/test/data/activity-calendar/";
        File file = new File(basePath, "activity-calendars-list-small-test.csv");
        assertLoadFromFile(file);
    }

    @Test
    public void testImportListActivityCalendars() {
        String fileName = "activity-calendars-list-small-test.csv";
        String basePath = "src/test/data/activity-calendar/";
        File file = new File(basePath, fileName);

        ListActivityImportCalendarContextVO context = ListActivityImportCalendarContextVO.builder()
                .recorderPersonId(1)
                .processingFile(file)
                .build();

        Future<ListActivityCalendarImportResultVO> future = listActivityCalendarImportService.asyncImportFromFile(context, null);
        try {

            ListActivityCalendarImportResultVO result = future.get();

            ListActivityCalendarImportResultVO expectedResult = new ListActivityCalendarImportResultVO();
            expectedResult.setStatus(JobStatusEnum.SUCCESS);
            expectedResult.setMessage("Import successful");
            expectedResult.setInserts(10);
            expectedResult.setUpdates(5);
            expectedResult.setDisables(2);
            expectedResult.setWarnings(1);
            expectedResult.setErrors(0);

            assertAll("Result",
                    () -> assertEquals(expectedResult.getStatus(), result.getStatus()),
                    () -> assertEquals(expectedResult.getMessage(), result.getMessage()),
                    () -> assertEquals(expectedResult.getInserts(), result.getInserts()),
                    () -> assertEquals(expectedResult.getUpdates(), result.getUpdates()),
                    () -> assertEquals(expectedResult.getDisables(), result.getDisables()),
                    () -> assertEquals(expectedResult.getWarnings(), result.getWarnings()),
                    () -> assertEquals(expectedResult.getErrors(), result.getErrors())
            );

        } catch (InterruptedException | ExecutionException e) {

            e.printStackTrace();
        }
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
        } finally {
            Files.deleteTemporaryFiles(file);
        }
    }

    private int getAdminUserId() {
        return personService.findByFilter(PersonFilterVO.builder()
                        .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
                        .userProfileId(UserProfileEnum.ADMIN.getId())
                        .build(), Pageables.create(0, 1))
                .stream().findFirst().map(PersonVO::getId)
                .orElse(1); // Admin user
    }
}