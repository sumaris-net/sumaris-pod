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

import com.google.common.collect.Lists;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilege;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StrategyServiceWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private StrategyService service;

    @Autowired
    private ReferentialService referentialService;

    @Test
    public void saveExisting() {
        Integer strategyId = null;
        StrategyVO strategy = service.getByLabel("20LEUCCIR001",
                StrategyFetchOptions.builder()
                    .withPmfms(true)
                    .withTaxonNames(true)
                    .withPmfms(true)
                    .withDepartments(true)
                    .withAppliedStrategies(true)
                    .build());
        Assert.assertNotNull(strategy);
        Assert.assertNotNull(strategy.getId());
        strategyId = strategy.getId();
        Assert.assertNotNull(strategy.getTaxonNames());
        Assert.assertEquals(1, strategy.getTaxonNames().size());
        Assert.assertNotNull(strategy.getPmfms());
        Assert.assertEquals(12, strategy.getPmfms().size());
        Assert.assertNotNull(strategy.getAppliedStrategies());
        Assert.assertEquals(3, strategy.getAppliedStrategies().size());
        Assert.assertNotNull(strategy.getDepartments());
        Assert.assertEquals(2, strategy.getDepartments().size());

        // Modify name
        strategy.setName("Strategy Name changed");
        // Modify departments
        StrategyDepartmentVO strategyDepartment = new StrategyDepartmentVO();
        ReferentialVO privilege = new ReferentialVO();
        ReferentialVO department = new ReferentialVO();
        Beans.copyProperties(referentialService.get(ProgramPrivilege.class, 3), privilege);
        Beans.copyProperties(referentialService.get(Department.class, 3), department);
        strategyDepartment.setPrivilege(privilege);
        strategyDepartment.setDepartment(department);
        strategyDepartment.setId(-1);
        strategy.setDepartments(Lists.newArrayList(strategyDepartment));
        // Add an applied period
        AppliedPeriodVO appliedPeriod = new AppliedPeriodVO();
        appliedPeriod.setAppliedStrategyId(null);
        appliedPeriod.setStartDate(Dates.getFirstDayOfYear(2020));
        appliedPeriod.setEndDate(Dates.getFirstDayOfYear(2021));
        appliedPeriod.setAcquisitionNumber(10);
        strategy.getAppliedStrategies().get(1).setAppliedPeriods(Lists.newArrayList(appliedPeriod));
        strategy.getAppliedStrategies().get(2).setAppliedPeriods(Lists.newArrayList());

        service.save(strategy);

        // reload by id
        strategy = service.get(strategyId);
        Assert.assertNotNull(strategy);
        Assert.assertEquals("Strategy Name changed", strategy.getName());

        Assert.assertNotNull(strategy.getDepartments());
        Assert.assertEquals(1, strategy.getDepartments().size());
        StrategyDepartmentVO actualStrategyDepartment = strategy.getDepartments().get(0);
        Assert.assertEquals(privilege, actualStrategyDepartment.getPrivilege());
        Assert.assertEquals(department, actualStrategyDepartment.getDepartment());

        Assert.assertNotNull(strategy.getAppliedStrategies());
        Assert.assertEquals(3, strategy.getAppliedStrategies().size());
        List<AppliedPeriodVO> actualAppliedPeriods = strategy.getAppliedStrategies().get(1).getAppliedPeriods();
        Assert.assertNotNull(actualAppliedPeriods);
        Assert.assertEquals(1, actualAppliedPeriods.size());
        AppliedPeriodVO actualAppliedPeriod = actualAppliedPeriods.get(0);
        Assert.assertNotNull(actualAppliedPeriod);
        Assert.assertEquals(Dates.getFirstDayOfYear(2020), actualAppliedPeriod.getStartDate());
        Assert.assertEquals(Dates.getFirstDayOfYear(2021), actualAppliedPeriod.getEndDate());
        Assert.assertEquals(10, actualAppliedPeriod.getAcquisitionNumber().intValue());
    }

    @Test
    public void saveNew() {
        StrategyVO strategy = new StrategyVO();
        strategy.setLabel("STRAT-TEST");
        strategy.setName("label test");
        strategy.setStatusId(StatusEnum.TEMPORARY.getId());
        strategy.setProgramId(40);

        service.save(strategy);
    }

    @Test
    public void z_delete() {
        service.delete(32);
    }

}
