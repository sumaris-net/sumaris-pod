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
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationClassificationEnum;
import net.sumaris.core.model.referential.pmfm.Fraction;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.programStrategy.StrategyPredocService;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class StrategyPredocServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private StrategyPredocService service;

    @Test
    public void findStrategiesReferentials() {
        List<ReferentialVO> results = service.findStrategiesReferentials("AnalyticReference", 40, 0, 10);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesReferentials(Department.class.getSimpleName(), 40, 0, 10);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesReferentials(Location.class.getSimpleName(), 40, 0, 10);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesReferentials(Location.class.getSimpleName(), 40, LocationClassificationEnum.SEA, 0, 10);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesReferentials(TaxonName.class.getSimpleName(), 40, 0, 10);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesReferentials(Pmfm.class.getSimpleName(), 40, 0, 10);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesReferentials(Fraction.class.getSimpleName(), 40, 0, 10);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));
    }

    @Test
    public void findStrategiesValues() {
        List<String> refs = service.findStrategiesAnalyticReferences(40);
        Assert.assertNotNull(refs);
        Assert.assertTrue(CollectionUtils.isNotEmpty(refs));
        Assert.assertTrue(refs.contains("P101-0001-01-DF"));

        List<Integer> results = service.findStrategiesDepartments(40);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesLocations(40, null);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesLocations(40, LocationClassificationEnum.SEA);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesTaxonNames(40);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesPmfms(40, null, PmfmStrategy.Fields.PMFM);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesPmfms(40, 1006, PmfmStrategy.Fields.PMFM);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        results = service.findStrategiesPmfms(40, 1006, PmfmStrategy.Fields.FRACTION);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));
    }

}
