/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.service.technical;

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.ProcessingTypeEnum;
import net.sumaris.core.model.social.SystemRecipientEnum;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.technical.JobService;
import net.sumaris.core.vo.technical.job.JobVO;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class JobServiceWriteTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private JobService service;


    @Test
    public void saveInvalid() {

        // Invalid job (no name)
        try {
            JobVO job = createSystemJob();
            job.setName(null);
            service.save(job);
            Assert.fail("Job's name should be mandatory");
        } catch (Exception e) {
            // Continue
        }

        // Invalid job (no status)
        try {
            JobVO job = createSystemJob();
            job.setStatus(null);
            service.save(job);
            Assert.fail("Job's status should be mandatory");
        } catch (Exception e) {
            // Continue
        }

        // Invalid job (no type)
        try {
            JobVO job = createSystemJob();
            job.setType(null);
            service.save(job);
            Assert.fail("Job's type should be mandatory");
        } catch (Exception e) {
            // Continue
        }
    }

    @Test
    //@Ignore
    // FIXME failed with a transaction error ??
    public void save() {

        JobVO job = createSystemJob();
        JobVO savedJob = service.save(job);

        Assert.assertNotNull(savedJob);
        Assert.assertNotNull(savedJob.getId());
        Assert.assertNotNull(savedJob.getName());

        // Reload
        JobVO reloadedJob = service.get(savedJob.getId());
        Assert.assertNotNull(reloadedJob);
        Assert.assertNotNull(reloadedJob.getId());
        Assert.assertNotNull(reloadedJob.getName());
    }

    /* -- internal functions -- */

    private JobVO createSystemJob() {
        return this.createJob(SystemRecipientEnum.SYSTEM.getLabel());
    }

    private JobVO createJob(String issuer) {
        return JobVO.builder()
            .name("Extraction test job")
            .type(ProcessingTypeEnum.SIOP_VESSELS_IMPORTATION.getLabel())
            .issuer(issuer)
            .status(JobStatusEnum.PENDING)
            .build();
    }
}
