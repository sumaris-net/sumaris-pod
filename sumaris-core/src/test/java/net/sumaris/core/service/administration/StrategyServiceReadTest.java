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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.PeriodVO;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class StrategyServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private StrategyService service;

    @Test
    public void getGears() {

        ProgramVO defaultProg =  fixtures.getDefaultProgram();

        List<ReferentialVO> results = service.getGears(defaultProg.getId());
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() > 10);
        Assert.assertTrue(results.size() < 30);
    }

    @Test
    public void getTaxonGroupStrategies() {

        ProgramVO program =  fixtures.getAuctionProgram();

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
    public void findPmfmsByFilter() {

        // Get by strategy
        List<PmfmStrategyVO> pmfmStrategies = service.findPmfmsByFilter(PmfmStrategyFilterVO.builder()
                .strategyId(1)
                .build(), PmfmStrategyFetchOptions.DEFAULT);
        Assert.assertNotNull(pmfmStrategies);
        Assert.assertEquals(80, pmfmStrategies.size());
        PmfmStrategyVO pmfmStrategy = pmfmStrategies.get(0);
        Assert.assertNotNull(pmfmStrategy);
        Assert.assertNotNull(pmfmStrategy.getPmfmId());
        Assert.assertNull(pmfmStrategy.getPmfm());

        // Get by program and acquisition level
        pmfmStrategies = service.findPmfmsByFilter(PmfmStrategyFilterVO.builder()
                .programId(fixtures.getDefaultProgram().getId())
                .acquisitionLevelId(AcquisitionLevelEnum.TRIP.getId())
                .build(), PmfmStrategyFetchOptions.DEFAULT);
        Assert.assertNotNull(pmfmStrategies);
        Assert.assertEquals(5, pmfmStrategies.size());

    }

    @Test
    public void findDenormalizedPmfmsByFilter() {

        List<DenormalizedPmfmStrategyVO> pmfms = service.findDenormalizedPmfmsByFilter(
                PmfmStrategyFilterVO.builder()
                    .strategyId(1)
                    .build(),
                PmfmStrategyFetchOptions.builder()
                        .withCompleteName(true)
                        .build());
        Assert.assertNotNull(pmfms);
        Assert.assertEquals(80, pmfms.size());

        long count = pmfms.stream().filter(pmfm -> {
            Assert.assertNotNull(pmfm);
            Assert.assertNotNull(pmfm.getId());
            Assert.assertNotNull(pmfm.getType());
            Assert.assertNotNull(pmfm.getCompleteName());
            return pmfm.getUnitLabel() != null;
        }).count();

        Assert.assertTrue(count > 0);
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
    public void computeNextLabel() {

        // By programId
        String label = service.computeNextLabelByProgramId(40, "BIO", 0);
        Assert.assertEquals("BIO1", label);

        label = service.computeNextLabelByProgramId(40, "20LEUCCIR", 3);
        Assert.assertEquals("20LEUCCIR003", label);

        // By strategyLabel
        label = service.computeNextSampleLabelByStrategy("BIO1", "-", 0);
        Assert.assertEquals("BIO1-1", label);

        label = service.computeNextSampleLabelByStrategy("20LEUCCIR001", "-", 4);
        Assert.assertEquals("20LEUCCIR001-0005", label);
    }


    @Test
    public void findByFilter() {

        Page page = Page.builder().size(100).build();

        // Filter by program
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                .programLabels(new String[]{fixtures.getDefaultProgram().getLabel()})
                .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(1, strategies.size());
        }

        // Filter by analytic reference
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                    .analyticReferences(new String[]{"P101-0001-01-DF"})
                    .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(1, strategies.size());
            Assert.assertEquals("20LEUCCIR001", strategies.get(0).getLabel());
        }

        // Filter by reference taxon
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                .referenceTaxonIds(new Integer[]{1006})
                .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(2, strategies.size());
            Assert.assertEquals("20LEUCCIR001", strategies.get(0).getLabel());
        }

        // Filter by department
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                    .departmentIds(new Integer[]{3})
                    .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(1, strategies.size());
            Assert.assertEquals("20LEUCCIR001", strategies.get(0).getLabel());
        }

        // Filter by location
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                    .locationIds(new Integer[]{101})
                    .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(1, strategies.size());
            Assert.assertEquals("20LEUCCIR001", strategies.get(0).getLabel());
        }

        // Filter by pmfm
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                    .parameterIds(new Integer[]{350, 351})
                    .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(2, strategies.size());
            Assert.assertEquals("20LEUCCIR001", strategies.get(0).getLabel());
            Assert.assertEquals("20LEUCCIR002", strategies.get(1).getLabel());
        }

        // Filter by periods
        {
            StrategyFilterVO filter = StrategyFilterVO.builder()
                    .periods(new PeriodVO[]{PeriodVO.builder()
                            .startDate(Dates.safeParseDate("2020-01-01", "yyyy-MM-dd"))
                            .endDate(Dates.safeParseDate("2020-03-31", "yyyy-MM-dd"))
                            .build(),
                    })
                    .build();
            List<StrategyVO> strategies = service.findByFilter(filter, page, StrategyFetchOptions.DEFAULT);
            Assert.assertNotNull(strategies);
            Assert.assertEquals(1, strategies.size());
            Assert.assertEquals("20LEUCCIR001", strategies.get(0).getLabel());
        }
    }
}
