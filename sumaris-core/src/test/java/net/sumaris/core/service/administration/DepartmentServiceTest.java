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
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.DepartmentFilterVO;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DepartmentServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private DepartmentService service;

    @Test
    public void a_findDepartments() {

        // without filter
        assertFindResult(null, 12);

        // Find with logo = false
        DepartmentFilterVO filter = new DepartmentFilterVO();
        filter.setWithLogo(false);
        assertFindResult(filter, 12); // should be same as previous

        // Find with logo = true
        filter.setWithLogo(true);
        assertFindResult(filter, 10); // should be same as previous

    }

    private void assertFindResult(DepartmentFilterVO filter, int expectedSize) {
        List<DepartmentVO> results = service.findByFilter(filter, 0, 100, null, null);
        Assert.assertNotNull(results);
        Assert.assertEquals(expectedSize, results.size());
    }

    @Test
    public void b_save() {
        DepartmentVO vo = new DepartmentVO();
        vo.setLabel("dep label");
        vo.setName("dep name");
        vo.setSiteUrl("http://www.sumaris.net");
        vo.setStatusId(StatusEnum.ENABLE.getId());
        service.save(vo);

        // find
        assertFindResult(null, 12); // 11 + the new one
        // now with logo
        DepartmentFilterVO filter = new DepartmentFilterVO();
        filter.setWithLogo(true);
        assertFindResult(filter, 10); // only the 10 initial with logo

    }

    @Test
    public void c_delete() {
        service.delete(fixtures.getDepartmentId(1));
    }

    @Test
    public void d_getLogo() {
        ImageAttachmentVO image = service.getLogoByLabel("Ifremer");
        Assert.assertNotNull(image);

        try {
            service.getLogoByLabel("_____");
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }
}
