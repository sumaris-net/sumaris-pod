package net.sumaris.core.service.referential;

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
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ReferentialServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private ReferentialService service;

    @Test
    public void findByFilter() {
        ReferentialFilterVO filter = new ReferentialFilterVO();

        filter.setSearchText("FRA");
        filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId()});

        List<ReferentialVO> results = service.findByFilter(Location.class.getSimpleName(), filter, 0, 100);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        filter.setLevelIds(new Integer[]{-999});
        results = service.findByFilter(Location.class.getSimpleName(), filter, 0, 100);
        Assert.assertTrue(CollectionUtils.isEmpty(results));
    }

    @Test
    public void findByFilterWithStatus() {
        ReferentialFilterVO filter = new ReferentialFilterVO();

        filter.setSearchText("FRA");
        filter.setStatusIds(new Integer[]{StatusEnum.DISABLE.getId()});

        List<ReferentialVO> results = service.findByFilter(Location.class.getSimpleName(), filter, 0, 100);
        Assert.assertTrue(CollectionUtils.isEmpty(results));
    }
}
