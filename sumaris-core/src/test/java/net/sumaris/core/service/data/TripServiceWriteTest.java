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
import com.google.common.collect.ImmutableSet;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.referential.LocationVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

public class TripServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private TripService service;

    @Autowired
    private OperationService operationService;

    @Autowired
    private PmfmService pmfmService;


    @Test
    public void save() {
        TripVO vo = createTrip();
        TripVO savedVO = service.save(vo, null);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload and check
        TripVO reloadedVO = service.get(savedVO.getId());
        Assert.assertNotNull(reloadedVO);

        // Observers
        Assert.assertNotNull(reloadedVO.getObservers());
        Assert.assertEquals(2, reloadedVO.getObservers().size());

    }

    @Test
    public void saveWithOperation() {
        // Create a trip, with an physical gear
        TripVO trip = createTrip();

        // Create a operation, with an physical gear having a rankOrder, without id
        // (can occur when saving a trip WITH operation for the first time)
        OperationVO operation = createOperation(trip);
        PhysicalGearVO opeGear = new PhysicalGearVO();
        opeGear.setRankOrder(1);
        operation.setPhysicalGear(opeGear);
        operation.setPhysicalGearId(null);

        trip.setOperations(ImmutableList.of(operation));

        TripVO savedVO = service.save(trip, TripSaveOptions.builder().withOperation(true).build());

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload and check
        List<OperationVO> savedOperations = operationService.findAllByTripId(savedVO.getId(), OperationFetchOptions.DEFAULT);
        Assert.assertNotNull(savedOperations);
        Assert.assertEquals(1, savedOperations.size());

        OperationVO saveOperation = savedOperations.get(0);
        Assert.assertNotNull(saveOperation.getPhysicalGear());
        Assert.assertNotNull(saveOperation.getPhysicalGear().getId());

        Assert.assertEquals("Operation's physical gear id should be equals to trip's physical gear",
                savedVO.getGears().get(0).getId(), saveOperation.getPhysicalGear().getId());

    }

    @Test
    public void delete() {
        TripVO savedVO = null;
        try {
            // Create trip
            savedVO = service.save(createTrip(), null);
            Assume.assumeNotNull(savedVO);
            Assume.assumeNotNull(savedVO.getId());

            // Create operation
            OperationVO op = DataTestUtils.createOperation(fixtures, pmfmService, savedVO);
            op.setPhysicalGear(savedVO.getGears().get(0));
            savedVO.setOperations(List.of(op));
            savedVO = service.save(createTrip(), TripSaveOptions.builder()
                    .withOperation(true)
                    .build());

        }
        catch(Exception e) {
            Assume.assumeNoException(e);
        }

        if (savedVO != null) {
            service.asyncDelete(savedVO.getId());
        }
    }

    /* -- Protected -- */

    protected TripVO createTrip() {
        return DataTestUtils.createTrip(fixtures, pmfmService);
    }

    protected OperationVO createOperation(TripVO parent) {
        return DataTestUtils.createOperation(fixtures, pmfmService, parent);
    }

}
