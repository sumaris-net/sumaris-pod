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
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.TaxonGroupStrategyVO;
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

        ProgramVO defaultProg =  dbResource.getFixtures().getDefaultProgram();

        List<ReferentialVO> results = service.getGears(defaultProg.getId());
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() > 10);
        Assert.assertTrue(results.size() < 30);
    }

    @Test
    public void getTaxonGroupStrategies() {

        ProgramVO program =  dbResource.getFixtures().getAuctionProgram();

        List<TaxonGroupStrategyVO> results = service.getTaxonGroupStrategies(program.getId());
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() > 0);
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
    public void findPmfmStrategiesByProgramAndAcquisitionLevel() {

        List<PmfmStrategyVO> pmfmStrategies = service.findPmfmStrategiesByProgramAndAcquisitionLevel(dbResource.getFixtures().getDefaultProgram().getId(), 2, StrategyFetchOptions.builder().build());
        Assert.assertNotNull(pmfmStrategies);
        Assert.assertEquals(24, pmfmStrategies.size());

    }

}
