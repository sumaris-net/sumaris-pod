package net.sumaris.core.extraction.service;

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

import net.sumaris.core.extraction.DatabaseResource;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.specification.data.trip.RdbSpecification;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.extraction.util.ExtractionProducts;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

/**
 * @author benoit.lavenier@e-is.pro
 */
public class ExtractionProductServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ExtractionProductService service;

    @Test
    public void getAndSave() {
        ExtractionProductVO source = service.getByLabel(fixtures.getRdbProductLabel(0), ExtractionProductFetchOptions.FOR_UPDATE);

        source.setComments("Test save");

        // Save
        ExtractionProductVO target = service.save(source);
        Assert.assertNotNull(target);
        Assert.assertNotNull(target.getId());
    }

    @Test
    public void saveThenDelete() {
        ExtractionProductVO source = createProduct(ExtractionCategoryEnum.LIVE, LiveFormatEnum.SURVIVAL_TEST);

        // Save
        ExtractionProductVO target = service.save(source);
        Assert.assertNotNull(target);
        Assert.assertNotNull(target.getId());

        // Delete
        service.delete(target.getId());
    }

    @Test
    @Ignore
    // FIXME BLA: regarder si on doit compter ou supprimer les colonnes hidden ?
    public void getColumnsBySheetName() {

        ExtractionProductVO source = service.getByLabel(fixtures.getRdbProductLabel(0), ExtractionProductFetchOptions.TABLES_AND_STRATUM);
        String sheetName = RdbSpecification.HH_SHEET_NAME;

        // Check columns
        List<ExtractionTableColumnVO> columns = service.getColumnsBySheetName(source.getId(), sheetName, ExtractionTableColumnFetchOptions.FULL);
        Assert.assertNotNull(columns);
        Assert.assertTrue(columns.size() > 0);

        // Check columns order
        String[] orderedColumns = ExtractionTableColumnOrder.COLUMNS_BY_SHEET.get(ExtractionTableColumnOrder.key(RdbSpecification.FORMAT, sheetName));

        // Remove the record_type
        orderedColumns = Arrays.copyOfRange(orderedColumns, 1, orderedColumns.length);

        for(ExtractionTableColumnVO column: columns) {
            Assert.assertNotNull(column.getRankOrder());
            Assert.assertNotNull(column.getColumnName());
            Assert.assertEquals(column.getColumnName().toLowerCase(), column.getColumnName());

            int index = ArrayUtils.indexOf(orderedColumns, column.getColumnName());
            if (index != -1) {
                Assert.assertEquals(index, column.getRankOrder().intValue());
            }
        }
    }

    /* -- protected methods --*/

    protected ExtractionProductVO createProduct(ExtractionCategoryEnum category, IExtractionFormat format) {

        ExtractionProductVO target = new ExtractionProductVO();
        target.setLabel(ExtractionProducts.getProductLabel(format, System.currentTimeMillis()));
        target.setFormat(format.getLabel());
        target.setVersion(format.getVersion());
        target.setName(String.format("Product on %s (%s) data", format.getLabel(), category.name()));
        target.setStatusId(StatusEnum.TEMPORARY.getId());

        DepartmentVO recDep = new DepartmentVO();
        recDep.setId(fixtures.getDepartmentId(0));
        target.setRecorderDepartment(recDep);

        PersonVO recorder = new PersonVO();
        recorder.setId(fixtures.getPersonId(0));
        target.setRecorderPerson(recorder);

        return target;
    }
}
