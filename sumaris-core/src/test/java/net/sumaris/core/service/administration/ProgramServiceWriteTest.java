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
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.util.Lists;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProgramServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ProgramService service;

    @Autowired
    private StrategyService strategyService;

    @Autowired
    private ReferentialService referentialService;

    @Test
    public void saveExisting() {
        ProgramVO program = service.getByLabel("ADAP-CONTROLE");
        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getId());
        Assert.assertEquals(11, program.getId().intValue());
        Assert.assertNotNull(program.getProperties());
        Assert.assertEquals(6, program.getProperties().size());
        Assert.assertNull(program.getStrategies()); // no strategy

        // Modify name
        program.setName("Program Name changed");
        // Add a property
        program.getProperties().put("PROPERTY_TEST", "PROPERTY_VALUE");

        service.save(program, null);

        // reload by id
        program = service.get(11);
        Assert.assertNotNull(program);
        Assert.assertEquals("Program Name changed", program.getName());
        Assert.assertNotNull(program.getProperties());
        Assert.assertEquals(7, program.getProperties().size());
        Assert.assertEquals("PROPERTY_VALUE", program.getProperties().get("PROPERTY_TEST"));

        Assert.assertNull(program.getStrategies()); // no strategy
    }

    @Test
    public void saveWithStrategies() {
        ProgramVO program = service.getByLabel("SIH-OBSBIO");
        Assert.assertNotNull(program);
        Assert.assertNotNull(program.getId());
        Assert.assertEquals(40, program.getId().intValue());

        StrategyFetchOptions fetchOptions = StrategyFetchOptions.builder()
            .withPmfms(true)
            .withAppliedStrategies(true).build();
        // Modify strategies
        //strategies = program.getStrategies();
        List<StrategyVO> strategies = strategyService.findByProgram(program.getId(), fetchOptions);
        List<AppliedStrategyVO> appliedStrategies = Lists.newArrayList();
        List<AppliedPeriodVO> appliedPeriods = Lists.newArrayList();
        Assert.assertNotNull(strategies);
        Assert.assertTrue(strategies.size() > 0);
        for (StrategyVO strategy : strategies) {
            if (strategy.getId() == 30) {
                appliedStrategies = strategy.getAppliedStrategies();
                Assert.assertNotNull(appliedStrategies);
                Assert.assertTrue(appliedStrategies.size() > 0);
                strategy.setAnalyticReference("Reference changed");
            }
        }
        for (AppliedStrategyVO appliedStrategy : appliedStrategies) {
            if (appliedStrategy.getId() == 10) {
                appliedPeriods = appliedStrategy.getAppliedPeriods();
                Assert.assertNotNull(appliedPeriods);
                Assert.assertEquals(3, appliedPeriods.size());

                LocationVO location = new LocationVO();
                Beans.copyProperties(referentialService.get(Location.class, 23), location);
                appliedStrategy.setLocation(location);
            }
        }
        for (AppliedPeriodVO appliedPeriod : appliedPeriods) {
            Assert.assertNotNull(appliedPeriod.getStartDate());
            Assert.assertNotNull(appliedPeriod.getEndDate());
            appliedPeriod.setAcquisitionNumber(1);
        }
        program.setStrategies(strategies);

        service.save(program, null);

        // reload by id
        ProgramVO actualProgram = service.get(40);
        Assert.assertNotNull(actualProgram);
        Assert.assertNotNull(actualProgram.getId());

        //strategies = program.getStrategies();
        List<StrategyVO> reloadedStrategies = strategyService.findByProgram(actualProgram.getId(), fetchOptions);
        Assert.assertEquals(strategies.size(), reloadedStrategies.size());
        reloadedStrategies.forEach(actualStrategy -> {
            Assert.assertNotNull(actualStrategy.getId());
            StrategyVO expectedStrategy = strategies.stream().filter(s -> actualStrategy.getId().equals(s.getId()))
                .findFirst().orElse(null);
            Assert.assertNotNull(expectedStrategy);
            Assert.assertEquals(expectedStrategy.getLabel(), actualStrategy.getLabel());
            Assert.assertEquals(
                CollectionUtils.size(expectedStrategy.getAppliedStrategies()),
                CollectionUtils.size(actualStrategy.getAppliedStrategies()));
        });
    }

    @Test
    public void saveNew() {

        ProgramVO program = new ProgramVO();
        program.setLabel("PROG-TEST");
        program.setName("label test");
        program.setStatusId(StatusEnum.TEMPORARY.getId());
        ReferentialVO gearClassification = new ReferentialVO();
        gearClassification.setId(1);
        program.setGearClassification(gearClassification);
        ReferentialVO taxonGroupType = new ReferentialVO();
        taxonGroupType.setId(2);
        program.setTaxonGroupType(taxonGroupType);

        service.save(program, null);
    }

    @Test
    public void z_delete() {

        try {
            service.delete(11);
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
            // TODO this service delete should delete also children entities...
        }

    }

}
