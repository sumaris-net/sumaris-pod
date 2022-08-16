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

package net.sumaris.core.service.data.vessel;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.data.VesselVO;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

public class VesselServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private VesselService service;

    @Test
    public void save() {
        int vesselId = fixtures.getVesselId(0);

        // declare a feature change (vessel1 represent previous version, vessel2 the new version)
        VesselVO vessel1 = service.get(vesselId);
        Assert.assertNotNull(vessel1);
        Assert.assertNotNull(vessel1.getVesselFeatures());
        Assert.assertEquals(2, vessel1.getVesselFeatures().getId().intValue());
        Assert.assertNotNull(vessel1.getVesselFeatures().getStartDate());
        Assert.assertNull(vessel1.getVesselFeatures().getEndDate());
        Assert.assertNotNull(vessel1.getVesselFeatures().getExteriorMarking());
        Assert.assertEquals("Vessel 1", vessel1.getVesselFeatures().getName());

        VesselVO vessel2 = service.get(vesselId);
        Assert.assertNotNull(vessel2);

        // close period
        Date changeDate = DateUtils.addMonths(vessel1.getVesselFeatures().getStartDate(), 1);
        vessel1.getVesselFeatures().setEndDate(DateUtils.addSeconds(changeDate, -1));

        // declare new period
        vessel2.getVesselFeatures().setId(null);
        vessel2.getVesselFeatures().setStartDate(changeDate);
        vessel2.getVesselFeatures().setName("new name");

        List<VesselVO> savedVessels = service.save(ImmutableList.of(vessel1, vessel2));
        Assert.assertNotNull(savedVessels);
        Assert.assertEquals(2, savedVessels.size());
        VesselVO savedVessel1 = savedVessels.get(0);
        VesselVO savedVessel2 = savedVessels.get(1);

        Assert.assertEquals(1, savedVessel1.getVesselFeatures().getId().intValue());
        Assert.assertNotEquals(1, savedVessel2.getVesselFeatures().getId().intValue());
        int featuresId2 = savedVessel2.getVesselFeatures().getId();
        Assert.assertEquals("Vessel 1", savedVessel1.getVesselFeatures().getName());
        Assert.assertEquals("new name", savedVessel2.getVesselFeatures().getName());

        // read features history
        Page<VesselFeaturesVO> featuresPage = service.getFeaturesByVesselId(vessel1.getId(), Pageables.create(0, 10, "id", SortDirection.ASC),
            DataFetchOptions.MINIMAL);
        Assert.assertNotNull(featuresPage);
        Assert.assertFalse(featuresPage.isEmpty());
        VesselFeaturesVO[] features = featuresPage.get().toArray(VesselFeaturesVO[]::new);

        Assert.assertNotNull(features[0]);
        Assert.assertEquals(1, features[0].getId().intValue());

        Assert.assertNotNull(features[1]);
        Assert.assertEquals(featuresId2, features[1].getId().intValue());

    }

}
