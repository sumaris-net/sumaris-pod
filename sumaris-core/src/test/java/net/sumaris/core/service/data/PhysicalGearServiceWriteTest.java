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
import com.google.common.collect.ImmutableMap;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.model.TreeNodeEntities;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.PhysicalGearVO;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PhysicalGearServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private PhysicalGearService service;

    @Test
    public void saveWithChildren() {

        int tripId = fixtures.getTripIdWithSubGears();
        List<PhysicalGearVO> gears = service.getAllByTripId(tripId, DataFetchOptions.builder()
                .withChildrenEntities(true)
                .build());
        Assume.assumeTrue(gears.size() == 4);
        PhysicalGearVO rootGear = TreeNodeEntities.listAsTree(gears, PhysicalGearVO::getParentId, true);

        // Add a sub gear
        PhysicalGearVO newChild = createGear(4, "New sub-gear");
        rootGear.getChildren().add(newChild);

        // Save
        service.saveAllByTripId(tripId, ImmutableList.of(rootGear));

        // Reload
        {
            List<PhysicalGearVO> reloadGears = service.getAllByTripId(tripId, DataFetchOptions.builder()
                .withChildrenEntities(true)
                .build());
            Assert.assertEquals(gears.size() + 1, reloadGears.size());
        }
    }


    protected PhysicalGearVO createGear(int rankOrder, String label) {
        PhysicalGearVO target = new PhysicalGearVO();
        target.setRankOrder(rankOrder);
        target.setMeasurementValues(ImmutableMap.of(PmfmEnum.GEAR_LABEL.getId(), label));

        return target;
    }

}