package net.sumaris.extraction.core.service;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTypeFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Benoit LAVENIER <benoit.lavenier@e-is.pro>
 */
public abstract class ExtractionTypeServiceReadTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private ExtractionTypeService service;

    @Test
    public void getByExample() {

        // Get valid live format
        {
            ExtractionTypeVO source = ExtractionTypeVO.builder()
                .format(LiveExtractionTypeEnum.RDB.getFormat())
                .category(LiveExtractionTypeEnum.RDB.getCategory())
                .build();

            IExtractionType type = service.getByExample(source);

            Assert.assertNotNull(type);
            Assert.assertNotNull(type.getFormat());
            Assert.assertEquals("type.format should be in uppercase", source.getFormat().toUpperCase(), type.getFormat());
            Assert.assertNotNull(type.getLabel());
            Assert.assertEquals("type.label is computed", LiveExtractionTypeEnum.RDB.getLabel(), type.getLabel());
            Assert.assertNotNull(type.getId());
            Assert.assertTrue("Enum id should be negative id", type.getId() < 0);
        }

        // Get invalid live format
        {
            ExtractionTypeVO format = new ExtractionTypeVO();
            format.setLabel("FAKE");
            format.setCategory(ExtractionCategoryEnum.LIVE);
            try {
                service.getByExample(format);
                Assert.fail("Should failed on wrong format");
            } catch (Exception e) {
                // OK
            }
        }

        // Get a valid product, by label
        {
            ExtractionTypeVO format = new ExtractionTypeVO();
            format.setLabel(fixtures.getRdbProductLabel(0));
            format.setCategory(ExtractionCategoryEnum.PRODUCT);
            IExtractionType type = service.getByExample(format);

            Assert.assertNotNull(type);
            Assert.assertEquals(format.getLabel(), type.getLabel());
            Assert.assertTrue(type instanceof ExtractionProductVO);
        }
    }

    @Test
    public void findByFilter() {

        ExtractionTypeFilterVO filter = ExtractionTypeFilterVO.builder().build();

        List<ExtractionTypeVO> types = service.findByFilter(filter, null);
        Assert.assertNotNull(types);
        Assert.assertTrue(types.size() > 0);
    }
}
