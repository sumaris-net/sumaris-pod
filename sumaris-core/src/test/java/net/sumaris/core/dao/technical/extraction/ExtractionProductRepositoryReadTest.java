package net.sumaris.core.dao.technical.extraction;

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
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.technical.extraction.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class ExtractionProductRepositoryReadTest extends AbstractDaoTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private ExtractionProductRepository repository;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete()
    }

    @Test
    public void findAll() {
        ExtractionTypeFilterVO filter = ExtractionTypeFilterVO.builder()
            //.statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
            .build();
        List<ExtractionProductVO> products = repository.findAll(filter);
        Assert.assertNotNull(products);
        Assert.assertEquals(1, products.size());
    }

}
