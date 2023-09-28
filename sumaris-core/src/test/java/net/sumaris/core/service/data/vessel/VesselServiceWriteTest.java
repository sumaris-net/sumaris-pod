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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.Entities;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.data.VesselVO;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class VesselServiceWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private VesselService service;

    @Test
    public void save() {
        VesselVO vessel1 = createVesselfromExisting(fixtures.getVesselId(0));
        vessel1 = service.save(vessel1);
        Assert.assertNotNull(vessel1);
        Assert.assertNotNull(vessel1.getId());

        int vesselId = vessel1.getId();

        // declare a feature change (vessel1 represent previous version, vessel2 the new version)
        Assert.assertNotNull(vessel1.getVesselFeatures());
        Assert.assertNotNull(vessel1.getVesselFeatures().getId());
        Assert.assertNotNull(vessel1.getVesselFeatures().getStartDate());
        Assert.assertNull(vessel1.getVesselFeatures().getEndDate());
        Assert.assertNotNull(vessel1.getVesselFeatures().getExteriorMarking());
        Assert.assertEquals("Navire 1", vessel1.getVesselFeatures().getName());

        VesselVO vessel2 = service.get(vesselId);
        Assert.assertNotNull(vessel2);

        // close period, on vessel 1
        Integer vessel1FeaturesId = vessel1.getVesselFeatures().getId();
        Date newEndDate = DateUtils.addMonths(vessel1.getVesselFeatures().getStartDate(), 1);
        newEndDate = DateUtils.addSeconds(newEndDate, -1);
        vessel1.getVesselFeatures().setEndDate(newEndDate);

        // declare new period, on vessel 2
        Integer vessel2FeaturesId = vessel2.getVesselFeatures().getId();
        vessel2.getVesselFeatures().setId(null);
        vessel2.getVesselFeatures().setStartDate(newEndDate);
        vessel2.getVesselFeatures().setUpdateDate(null);
        vessel2.getVesselFeatures().setCreationDate(null);
        vessel2.getVesselFeatures().setName("new name");

        List<VesselVO> savedVessels = service.save(ImmutableList.of(vessel1, vessel2));
        Assert.assertNotNull(savedVessels);
        Assert.assertEquals(2, savedVessels.size());
        VesselVO savedVessel1 = savedVessels.get(0);
        VesselVO savedVessel2 = savedVessels.get(1);

        Assert.assertEquals(vessel1FeaturesId, savedVessel1.getVesselFeatures().getId());
        Assert.assertNotEquals(vessel2FeaturesId, savedVessel2.getVesselFeatures().getId());
        Integer savedVessel2FeaturesId2 = savedVessel2.getVesselFeatures().getId();
        Assert.assertNotNull(savedVessel2FeaturesId2);
        Assert.assertEquals("Navire 1", savedVessel1.getVesselFeatures().getName());
        Assert.assertEquals("new name", savedVessel2.getVesselFeatures().getName());

        // read features history, and check all changes OK
        {
            List<VesselFeaturesVO> vessel1FeaturesPage = service.findFeaturesByVesselId(vesselId, net.sumaris.core.dao.technical.Page.create(0, 10, "id", SortDirection.ASC),
                DataFetchOptions.MINIMAL);
            Assert.assertNotNull(vessel1FeaturesPage);
            Assert.assertFalse(vessel1FeaturesPage.isEmpty());
            VesselFeaturesVO[] savedVesselFeatures = vessel1FeaturesPage.toArray(VesselFeaturesVO[]::new);

            VesselFeaturesVO savedFeatures1 = Arrays.stream(savedVesselFeatures)
                .filter(vf -> Objects.equals(vf.getId(), vessel1FeaturesId))
                .findFirst().orElseGet(null);
            Assert.assertNotNull(savedFeatures1);

            VesselFeaturesVO savedFeatures2 = Arrays.stream(savedVesselFeatures)
                .filter(vf -> Objects.equals(vf.getId(), vessel2FeaturesId))
                .findFirst().orElseGet(null);
            Assert.assertNotNull(savedFeatures2);
        }
    }

    @Test
    public void replaceTemporaryVessel() {

        // First, create a new temporary vessel
        // DO NOT replace the vessel#0, otherwise other test will failed !
        VesselVO tempVessel = createVesselfromExisting(fixtures.getVesselId(0));
        Assert.assertNotNull(tempVessel);
        Assert.assertEquals(StatusEnum.ENABLE.getId(), tempVessel.getStatusId());
        tempVessel.setStatusId(StatusEnum.TEMPORARY.getId());

        // Clear id/update date
        Entities.clearIdAndUpdateDate(tempVessel);
        Entities.clearIdAndUpdateDate(tempVessel.getVesselFeatures());
        tempVessel.getVesselFeatures().setVessel(tempVessel);
        Entities.clearId(tempVessel.getVesselRegistrationPeriod());
        tempVessel.getVesselRegistrationPeriod().setVessel(tempVessel);

        tempVessel = service.save(tempVessel);
        Assert.assertNotNull(tempVessel.getId());
        Assert.assertEquals(StatusEnum.TEMPORARY.getId(), tempVessel.getStatusId());

        // Replace it
        int replacementVesselId = fixtures.getVesselId(1);
        service.replaceTemporaryVessel(List.of(tempVessel.getId()), replacementVesselId);

    }

    protected VesselVO createVesselfromExisting(int vesselId) {
        VesselVO tempVessel = service.get(vesselId);
        Assert.assertNotNull(tempVessel);

        // Clear id/update date
        Entities.clearIdAndUpdateDate(tempVessel);
        Entities.clearIdAndUpdateDate(tempVessel.getVesselFeatures());
        tempVessel.getVesselFeatures().setVessel(tempVessel);
        Entities.clearId(tempVessel.getVesselRegistrationPeriod());
        tempVessel.getVesselRegistrationPeriod().setVessel(tempVessel);

        return tempVessel;
    }
}
