package net.sumaris.core.service.administration;

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

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class ProgramServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private ProgramService service;

    @Value("${spring.cache.enabled:false}")
    private boolean enableCache;

    @Test
    public void getById() {
        int programId = fixtures.getDefaultProgram().getId();
        ProgramVO program = service.get(programId);
        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getId());
        Assert.assertEquals(1, program.getId().intValue());

        Assert.assertNotNull(program.getProperties());
        Assert.assertEquals(9, program.getProperties().size());

        Assert.assertNotNull(program.getGearClassificationId());
        Assert.assertNotNull(program.getTaxonGroupTypeId());

        Assert.assertNull(program.getLocations());
        Assert.assertNull(program.getLocationIds());
        Assert.assertNull(program.getLocationClassifications());
        Assert.assertNull(program.getLocationClassificationIds());
    }

    @Test
    public void getByLabel() {
        ProgramVO program = service.getByLabel("ADAP-MER", ProgramFetchOptions.builder()
            .withProperties(true)
            .build());
        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getId());
        Assert.assertEquals(10, program.getId().intValue());

        Assert.assertNotNull(program.getProperties());
        Assert.assertEquals(22, program.getProperties().size());

        Assert.assertNotNull(program.getGearClassificationId());
        Assert.assertNotNull(program.getTaxonGroupTypeId());

        Assert.assertNull(program.getLocations());
        Assert.assertNull(program.getLocationIds());
        Assert.assertNull(program.getLocationClassifications());
        Assert.assertNull(program.getLocationClassificationIds());

        program = service.getByLabel("ADAP-MER", ProgramFetchOptions.builder()
            .withLocations(true)
            .withLocationClassifications(true)
            .build());
        Assert.assertNotNull(program.getLocations());
        Assert.assertNotNull(program.getLocationIds());
        Assert.assertNotNull(program.getLocationClassificationIds());
        Assert.assertNotNull(program.getLocationClassifications()); // ALways null, because filled by graphQL service
    }

    @Test
    public void findByFilter() {

        List<ProgramVO> programs = service.findByFilter(ProgramFilterVO.builder().searchText("ADAP").build(), 0,10, Program.Fields.LABEL, SortDirection.ASC);
        Assert.assertNotNull(programs);
        Assert.assertEquals(2, programs.size());
        Assert.assertEquals("ADAP-CONTROLE", programs.get(0).getLabel());
        Assert.assertEquals("ADAP-MER", programs.get(1).getLabel());

        programs = service.findByFilter(ProgramFilterVO.builder().withProperty("sumaris.trip.operation.batch.autoFill").build(), 0,10, Program.Fields.LABEL, SortDirection.ASC);
        Assert.assertNotNull(programs);
        Assert.assertEquals(3, programs.size());
        int[] expectedIds = new int[]{50, 10, 70};
        int[]  resultIds = programs.stream().mapToInt(ProgramVO::getId).toArray();
        Assert.assertArrayEquals(expectedIds, resultIds);
    }

    @Test
    public void getAll() {

        List<ProgramVO> programs = service.getAll();
        Assert.assertNotNull(programs);
        Assert.assertTrue(programs.size() >= 11);

    }

    @Test
    public void findAll() {

        List<ProgramVO> allPrograms = service.getAll();
        Assume.assumeTrue(!allPrograms.isEmpty());

        List<ProgramVO> enabledPrograms = service.findAll(ProgramFilterVO.builder()
            .statusIds(new Integer[]{StatusEnum.DISABLE.getId()})
            .build());
        Assert.assertNotNull(enabledPrograms);
        Assert.assertTrue(enabledPrograms.size() < allPrograms.size());

    }

    @Test
    public void getAcquisitionLevelsById() {
        int programId = fixtures.getDefaultProgram().getId();
        List<ReferentialVO> acquisitionLevels = service.getAcquisitionLevelsById(programId);

        Assert.assertNotNull(acquisitionLevels);
        Assert.assertTrue(acquisitionLevels.size() > 0);
    }

    @Test
    public void getAllPrivilegesByUserId() {
        int programId = fixtures.getDefaultProgram().getId();

        // No privilege user
        {
            int personId = fixtures.getPersonIdNoPrivilege();
            List<ProgramPrivilegeEnum> privileges = service.getAllPrivilegesByUserId(programId, personId);
            Assert.assertTrue(CollectionUtils.isEmpty(privileges));
        }

        // Observer user => OBSERVER privilege
        {
            int personId = fixtures.getPersonIdObserver();
            List<ProgramPrivilegeEnum> privileges = service.getAllPrivilegesByUserId(programId, personId);
            Assert.assertTrue(CollectionUtils.isNotEmpty(privileges));
            Assert.assertTrue(privileges.contains(ProgramPrivilegeEnum.OBSERVER));
        }

        // Supervisor user => MANAGER privilege
        {
            int personId = fixtures.getPersonIdManager();
            List<ProgramPrivilegeEnum> privileges = service.getAllPrivilegesByUserId(programId, personId);
            Assert.assertTrue(CollectionUtils.isNotEmpty(privileges));
            Assert.assertTrue(privileges.contains(ProgramPrivilegeEnum.MANAGER));
        }

        // Admin user => no privileges (but = ALL privileges)
        {
            int personId = fixtures.getPersonIdAdmin();
            List<ProgramPrivilegeEnum> privileges = service.getAllPrivilegesByUserId(programId, personId);
            Assert.assertTrue(CollectionUtils.isEmpty(privileges));
        }

        // Qualifier user
        {
            int personId = fixtures.getPersonIdQualifier();
            List<ProgramPrivilegeEnum> privileges = service.getAllPrivilegesByUserId(programId, personId);
            Assert.assertEquals(1, CollectionUtils.size(privileges));
            Assert.assertTrue(privileges.contains(ProgramPrivilegeEnum.QUALIFIER));
        }

        // Viewer user
        {
            int personId = fixtures.getPersonIdViewer();
            List<ProgramPrivilegeEnum> privileges = service.getAllPrivilegesByUserId(programId, personId);
            Assert.assertEquals(1, CollectionUtils.size(privileges));
            Assert.assertTrue(privileges.contains(ProgramPrivilegeEnum.VIEWER));
        }

        // Validator user
        {
            int personId = fixtures.getPersonIdValidator();
            long start = System.currentTimeMillis();
            List<ProgramPrivilegeEnum> privileges = service.getAllPrivilegesByUserId(programId, personId);
            long firstDuration = System.currentTimeMillis() - start;

            Assert.assertEquals(1, CollectionUtils.size(privileges));
            Assert.assertTrue(privileges.contains(ProgramPrivilegeEnum.VALIDATOR));

            // Test cache (if enable in properties)
            if (enableCache) {
                start = System.currentTimeMillis();
                service.getAllPrivilegesByUserId(programId, personId);
                long secondDuration = System.currentTimeMillis() - start;
                Assert.assertTrue(secondDuration < firstDuration);
            }
        }

    }

}
