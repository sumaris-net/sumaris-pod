package net.sumaris.core.dao.data;

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.data.operation.OperationRepository;
import net.sumaris.core.dao.data.sample.SampleRepository;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Slf4j
public class SampleRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private SampleRepository sampleRepository;

    private OperationVO parentOperation;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test

        parentOperation = operationRepository.get(1);
        Assume.assumeNotNull(parentOperation);
    }

    @Test
    public void save() {

        SampleVO sample = new SampleVO();
        sample.setOperationId(parentOperation.getId());
        sample.setComments("Sample #1 ope #" + parentOperation.getId());

        sample.setSampleDate(new Date());
        sample.setLabel("S1");
        sample.setRankOrder(1);
        sample.setIndividualCount(1);

        // Program
        sample.setProgram(fixtures.getDefaultProgram());

        // Matrix
        ReferentialVO matrix = new ReferentialVO();
        matrix.setId(fixtures.getMatrixIdForIndividual());
        sample.setMatrix(matrix);

        // Taxon group
        ReferentialVO taxonGroup= new ReferentialVO();
        taxonGroup.setId(fixtures.getTaxonGroupFAOId(0));
        sample.setTaxonGroup(taxonGroup);

        // Recorder department
        DepartmentVO recorderDepartment = new DepartmentVO();
        recorderDepartment.setId(fixtures.getDepartmentId(0));
        sample.setRecorderDepartment(recorderDepartment);

        SampleVO savedVO = sampleRepository.save(sample);
        Assert.assertNotNull(savedVO);
        Assert.assertNotNull(savedVO.getId());
    }

    @Test
    public void delete() {
        Integer id = fixtures.getSampleId(0);
        sampleRepository.deleteById(id);

    }

    @Test
    public void getWithImages() {

        // Get an existing sample, with images fetched
        SampleVO sample = sampleRepository.get(fixtures.getSampleIdWithImages(), SampleFetchOptions.builder()
                .withMeasurementValues(true)
                .withImages(true)
                .build());
        Assert.assertNotNull(sample);
        Assert.assertNotNull(sample.getId());
        Assert.assertTrue(CollectionUtils.isNotEmpty(sample.getImages()));
        Assert.assertEquals(1, CollectionUtils.size(sample.getImages()));
        sample.getImages().forEach(this::assertImage);

    }

    protected void assertImage(ImageAttachmentVO image) {
        Assert.assertNotNull(image);
        Assert.assertNotNull(image.getId());
        Assert.assertNotNull(image.getUpdateDate());
        Assert.assertNotNull(image.getCreationDate());
        Assert.assertEquals(ObjectTypeEnum.SAMPLE.getId(), image.getObjectTypeId());
        Assert.assertNotNull(image.getObjectId());
    }
}
