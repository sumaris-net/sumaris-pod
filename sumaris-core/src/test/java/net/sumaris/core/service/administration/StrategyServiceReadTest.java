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
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class StrategyServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private StrategyService service;

    @Test
    public void getGears() {

        ProgramVO defaultProg = dbResource.getFixtures().getDefaultProgram();

        List<ReferentialVO> results = service.getGears(defaultProg.getId());
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() > 10);
        Assert.assertTrue(results.size() < 30);
    }

    @Test
    public void getTaxonGroupStrategies() {

        ProgramVO program = dbResource.getFixtures().getAuctionProgram();

        List<TaxonGroupStrategyVO> results = service.getTaxonGroupStrategies(program.getId());
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() > 0);
    }

    @Test
    public void getAppliedStrategies() {

        List<AppliedStrategyVO> appliedStrategies = service.getAppliedStrategies(30);
        Assert.assertNotNull(appliedStrategies);
        Assert.assertTrue(appliedStrategies.size() > 0);
        AppliedStrategyVO appliedStrategy = appliedStrategies.get(0);
        Assert.assertNotNull(appliedStrategy.getLocation());
        List<AppliedPeriodVO> appliedPeriods = appliedStrategy.getAppliedPeriods();
        Assert.assertNotNull(appliedPeriods);
        Assert.assertEquals(3, appliedPeriods.size());
        AppliedPeriodVO appliedPeriod = appliedPeriods.get(0);
        Assert.assertNotNull(appliedPeriod.getStartDate());
    }

    @Test
    public void findPmfmStrategiesByStrategy() {

        List<PmfmStrategyVO> pmfmStrategies = service.findPmfmStrategiesByStrategy(1, StrategyFetchOptions.builder().build());
        Assert.assertNotNull(pmfmStrategies);
        Assert.assertEquals(80, pmfmStrategies.size());
        PmfmStrategyVO pmfmStrategy = pmfmStrategies.get(0);
        Assert.assertNotNull(pmfmStrategy);
        Assert.assertNotNull(pmfmStrategy.getPmfmId());
        Assert.assertNull(pmfmStrategy.getPmfm());

    }

    @Test
    public void getStrategyDepartments() {

        List<StrategyDepartmentVO> strategyDepartments = service.getStrategyDepartments(30);
        Assert.assertNotNull(strategyDepartments);
        Assert.assertTrue(strategyDepartments.size() > 0);
        StrategyDepartmentVO strategyDepartment = strategyDepartments.get(0);
        Assert.assertNotNull(strategyDepartment.getDepartment());
        Assert.assertNotNull(strategyDepartment.getPrivilege());

    }

    @Test
    public void findPmfmStrategiesByProgramAndAcquisitionLevel() {

        List<PmfmStrategyVO> pmfmStrategies = service.findPmfmStrategiesByProgramAndAcquisitionLevel(dbResource.getFixtures().getDefaultProgram().getId(), 2, StrategyFetchOptions.builder().build());
        Assert.assertNotNull(pmfmStrategies);
        Assert.assertEquals(24, pmfmStrategies.size());

    }

    @Test
    public void findNextLabelByProgramId() {
        String label = service.findNextLabelByProgramId(40, "BIO", 0);
        Assert.assertEquals("BIO1", label);

        label = service.findNextLabelByProgramId(40, "2020-BIO-", 4);
        Assert.assertEquals("2020-BIO-0003", label);
    }

}
