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
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.filter.DepartmentFilterVO;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DepartmentServiceTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private DepartmentService service;

    @Test
    public void findDepartments() {

        // Find with logo
        DepartmentFilterVO filter = new DepartmentFilterVO();
        filter.setWithLogo(Boolean.FALSE);
        List<DepartmentVO> results = service.findByFilter(filter, 0, 100, null, null);
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() > 0);
    }

    @Test
    @Ignore
    public void save() {
        DepartmentVO vo = new DepartmentVO();
        vo.setLabel("dep label");
        vo.setName("dep name");
        vo.setSiteUrl("http://www.sumaris.net");
        vo.setStatusId(getConfig().getStatusIdValid());

        service.save(vo);
    }

    @Test
    public void delete() {
        service.delete(dbResource.getFixtures().getDepartmentId(1));
    }
}
