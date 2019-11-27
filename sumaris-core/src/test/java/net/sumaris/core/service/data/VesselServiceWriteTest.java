package net.sumaris.core.service.data;

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

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.data.VesselVO;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

public class VesselServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private VesselService service;

    @Test
    public void save() {
        int vesselId = dbResource.getFixtures().getVesselId(0);

        // declare a feature change (vessel1 represent previous version, vessel2 the new version)
        VesselVO vessel1 = service.getVesselById(vesselId);
        Assert.assertNotNull(vessel1);
        Assert.assertNotNull(vessel1.getFeatures());
        Assert.assertEquals(1, vessel1.getFeatures().getId().intValue());
        Assert.assertNotNull(vessel1.getFeatures().getStartDate());
        Assert.assertNull(vessel1.getFeatures().getEndDate());
        Assert.assertNotNull(vessel1.getFeatures().getExteriorMarking());
        Assert.assertEquals("Vessel 1", vessel1.getFeatures().getName());

        VesselVO vessel2 = service.getVesselById(vesselId);
        Assert.assertNotNull(vessel2);

        // close period
        Date changeDate = DateUtils.addMonths(vessel1.getFeatures().getStartDate(), 1);
        vessel1.getFeatures().setEndDate(DateUtils.addSeconds(changeDate, -1));

        // declare new period
        vessel2.getFeatures().setId(null);
        vessel2.getFeatures().setStartDate(changeDate);
        vessel2.getFeatures().setName("new name");

        List<VesselVO> savedVessels = service.save(ImmutableList.of(vessel1, vessel2));
        Assert.assertNotNull(savedVessels);
        Assert.assertEquals(2, savedVessels.size());
        VesselVO savedVessel1 = savedVessels.get(0);
        VesselVO savedVessel2 = savedVessels.get(1);

        Assert.assertEquals(1, savedVessel1.getFeatures().getId().intValue());
        Assert.assertNotEquals(1, savedVessel2.getFeatures().getId().intValue());
        int featuresId2 = savedVessel2.getFeatures().getId();
        Assert.assertEquals("Vessel 1", savedVessel1.getFeatures().getName());
        Assert.assertEquals("new name", savedVessel2.getFeatures().getName());

        // read features history
        List<VesselFeaturesVO> features = service.getFeaturesByVesselId(vessel1.getId(), 0, 10, "id", SortDirection.ASC);
        Assert.assertNotNull(features);
        Assert.assertEquals(2, features.size());
        Assert.assertEquals(1, features.get(0).getId().intValue());
        Assert.assertEquals(featuresId2, features.get(1).getId().intValue());

    }

}
