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

import lombok.NonNull;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.OperationFilterVO;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OperationServiceReadTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private OperationService service;

    @Test
    public void findAllWithVesselIds() {
        assertFindAll(OperationFilterVO.builder()
            .vesselIds(new Integer[]{fixtures.getVesselId(1)})
            .build(), 0);

        assertFindAll(OperationFilterVO.builder()
            .vesselIds(new Integer[]{fixtures.getScientificVesselId()})
            .build(), 1);
    }

    /* -- Protected -- */

    protected List<OperationVO> assertFindAll(@NonNull OperationFilterVO filter, int expectedSize) {
        List<OperationVO> vos = service.findAllByFilter(filter, 0, 100, OperationVO.Fields.ID, SortDirection.ASC,
            OperationFetchOptions.DEFAULT);
        Assert.assertNotNull(vos);
        Assert.assertEquals(expectedSize, vos.size());
        return vos;
    }
}
