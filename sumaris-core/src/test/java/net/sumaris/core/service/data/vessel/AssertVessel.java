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

import com.google.common.collect.Sets;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.VesselVO;
import org.junit.Assert;

import java.util.Collection;
import java.util.Set;

public class AssertVessel {


    public static void assertValid(VesselVO source) {
        assertValid(source, AssertVesselSpecification.builder().build());
    }

    public static void assertValid(VesselVO source, AssertVesselSpecification spec) {
        Assert.assertNotNull(source);

        // Vessel type
        Assert.assertNotNull(source.getVesselType());
        Assert.assertNotNull(source.getVesselType().getId());

        // Features
        Assert.assertNotNull(source.getVesselFeatures());
        Assert.assertNotNull(source.getVesselFeatures().getName());
        if (spec.isRequiredExteriorMarking()) Assert.assertNotNull(source.getVesselFeatures().getExteriorMarking());
        if (spec.isRequiredBasePortLocation()) {
            Assert.assertNotNull(source.getVesselFeatures().getBasePortLocation());
            Assert.assertNotNull(source.getVesselFeatures().getBasePortLocation().getId());
        }

        // Registration period
        boolean hasRegistrationPeriod = source.getVesselRegistrationPeriod() != null; // can be null (.e.g no VesselRegistrationPeriod)
        if (hasRegistrationPeriod) {
            Assert.assertNotNull(source.getVesselRegistrationPeriod());
            Assert.assertNotNull(source.getVesselRegistrationPeriod().getId());
            Assert.assertNotNull(source.getVesselRegistrationPeriod().getRegistrationCode());
            Assert.assertNotNull(source.getVesselRegistrationPeriod().getRegistrationLocation());
            Assert.assertNotNull(source.getVesselRegistrationPeriod().getRegistrationLocation().getId());
        }
    }

    public static void assertValid(VesselSnapshotVO source) {
        assertValid(source, AssertVesselSpecification.builder().build());
    }

    public static void assertValid(VesselSnapshotVO source, AssertVesselSpecification spec) {
        Assert.assertNotNull(source);
        Assert.assertNotNull(source.getId());

        // Vessel type
        Assert.assertNotNull(source.getVesselType());
        Assert.assertNotNull(source.getVesselType().getId());

        // Features
        Assert.assertNotNull(source.getName());
        if (spec.isRequiredExteriorMarking()) Assert.assertNotNull(source.getExteriorMarking());
        if (spec.isRequiredBasePortLocation()) {
            Assert.assertNotNull(source.getBasePortLocation());
            Assert.assertNotNull(source.getBasePortLocation().getId());
        }

        // Registration period
        boolean hasRegistrationPeriod = source.getRegistrationLocation() != null; // can be null (.e.g no VesselRegistrationPeriod)
        if (hasRegistrationPeriod) {
            Assert.assertNotNull(source.getRegistrationLocation().getId());
            Assert.assertNotNull(source.getRegistrationCode());
        }
    }

    public static <E extends IEntity<Integer>> void assertAllValid(Collection<E> vessels) {
        assertAllValid(vessels, AssertVesselSpecification.builder().build());
    }

    public static <E extends IEntity<Integer>> void assertAllValid(Collection<E> vessels, AssertVesselSpecification spec) {
        for (E vessel: vessels) {
            if (vessel instanceof VesselVO) {
                AssertVessel.assertValid((VesselVO)vessel, spec);
            }
            else if (vessel instanceof VesselSnapshotVO) {
                AssertVessel.assertValid((VesselSnapshotVO)vessel, spec);
            }
        }
    }

    public static <E extends IEntity<Integer>> void assertUniqueIds(Collection<E> vessels) {
        final Set<Integer> ids = Sets.newHashSet();
        for (E vessel: vessels) {
            Assert.assertFalse("Duplicated vessel id=" + vessel.getId(), ids.contains(vessel.getId()));
            ids.add(vessel.getId());
        }
    }
}
