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
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.pmfm.PmfmService;
import net.sumaris.core.vo.data.*;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class SaleServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private SaleService service;

    @Autowired
    private OperationService operationService;

    @Autowired
    private PmfmService pmfmService;


    @Test
    public void save() {
        SaleVO vo = createSale();
        SaleVO savedVO = service.save(vo);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload and check
        SaleVO reloadedVO = service.get(savedVO.getId());
        Assert.assertNotNull(reloadedVO);

    }

    @Test
    public void saveWithMetiers() {
        SaleVO vo = createSale();
        vo.setMetiers(ImmutableList.of(DataTestUtils.createMetierVO(1)));
        SaleVO savedVO = service.save(vo);

        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());

        // Reload and check
        savedVO = service.get(savedVO.getId(), SaleFetchOptions.FULL_GRAPH);
        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getMetiers());
        Assert.assertEquals(savedVO.getMetiers().size(), 1);
        Assert.assertEquals(savedVO.getMetiers().get(0).getId(), (Integer)1);

        // Change metiers
        savedVO.setMetiers(ImmutableList.of(DataTestUtils.createMetierVO(2)));
        service.save(savedVO);
        savedVO = service.get(savedVO.getId(), SaleFetchOptions.FULL_GRAPH);
        Assert.assertEquals(savedVO.getMetiers().size(), 1);
        Assert.assertEquals(savedVO.getMetiers().get(0).getId(), (Integer)2);

    }

    /* -- Protected -- */

    protected SaleVO createSale() {
        return DataTestUtils.createSale(fixtures, pmfmService);
    }

}
