package net.sumaris.core.dao.technical;

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
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.model.technical.job.JobTypeEnum;
import net.sumaris.core.vo.technical.job.JobVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class JobRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private JobRepository repository;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete()
    }

    @Test
    public void save() {
        JobVO source = new JobVO();
        source.setName("Test job");
        source.setStatus(JobStatusEnum.PENDING);
        source.setType(JobTypeEnum.SIOP_VESSELS_IMPORTATION.name());
        source.setIssuer(JobVO.SYSTEM_ISSUER);

        // Save
        JobVO target = repository.save(source);
        Assert.assertNotNull(target);
        Assert.assertNotNull(target.getId());

        // Reload
//        JobVO reloadTarget = repository.getById(target.getId());
//        Assert.assertNotNull(reloadTarget);
//        Assert.assertNotNull(reloadTarget.getId());

    }

}
