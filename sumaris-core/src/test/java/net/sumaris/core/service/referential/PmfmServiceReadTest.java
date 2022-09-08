package net.sumaris.core.service.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author peck7 on 20/08/2020.
 */
public class PmfmServiceReadTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private PmfmService service;

    @Test
    public void findByFilter() {

        // unique label
        assertFindResult(ReferentialFilterVO.builder().label("CONVEYOR_BELT").build(), 1);
        // searchText
        assertFindResult(ReferentialFilterVO.builder().searchText("CONVEYOR_BELT").build(), 1);
        assertFindResult(ReferentialFilterVO.builder().searchText("NB").build(), 5);

        // levelLabels
        assertFindResult(ReferentialFilterVO.builder()
                .levelLabels(new String[]{"WEIGHT"})
                .build(), 6);

        // levelLabels + searchText
        assertFindResult(ReferentialFilterVO.builder()
                .levelLabels(new String[]{"WEIGHT"})
                .searchText("wei")
                .searchJoin("parameter")
                .build(), 6);
    }

    private void assertFindResult(ReferentialFilterVO filter, int expectedSize) {
        List<PmfmVO> pmfms = service.findByFilter(filter, 0, 100, "id", SortDirection.ASC, null);
        Assert.assertNotNull(pmfms);
        Assert.assertEquals(expectedSize, pmfms.size());
    }
}
