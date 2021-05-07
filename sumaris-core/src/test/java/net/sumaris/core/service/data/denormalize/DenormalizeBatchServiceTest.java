package net.sumaris.core.service.data.denormalize;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.data.batch.BatchRepository;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.data.DenormalizedBatchService;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.batch.BatchFetchOptions;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class DenormalizeBatchServiceTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private DenormalizedBatchService service;

    @Test
    public void denormalizeAndSaveByOperationId() {

        long startTime = System.currentTimeMillis();
        List<DenormalizedBatchVO> result = service.denormalizeAndSaveByOperationId(190136);

        //
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() > 0);
    }

}
