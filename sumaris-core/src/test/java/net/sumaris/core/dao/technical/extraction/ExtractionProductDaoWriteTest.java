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
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionProductColumn;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductColumnVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductTableVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ExtractionProductDaoWriteTest extends AbstractDaoTest{

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ExtractionProductDaoWriteTest.class);

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ExtractionProductDao dao;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test
    }

    @Test
    public void getAll() {
        List<ExtractionProductVO> products = dao.getAll();
        Assert.assertNotNull(products);
        Assert.assertTrue(products.size() > 0);
    }

    @Test
    public void delete() {
        Integer id = dbResource.getFixtures().getProductId(0);
        dao.delete(id);

    }

    @Test
    public void save() {
        ExtractionProductVO source = new ExtractionProductVO();
        source.setLabel("test");
        source.setName("Test product");
        source.setStatusId(StatusEnum.ENABLE.getId());

        List<ExtractionProductTableVO> tables = Lists.newArrayList();
        // TR
        {
            ExtractionProductTableVO table = new ExtractionProductTableVO();
            table.setLabel("TR");
            table.setName("Trip");
            table.setTableName("P01_RDB_TRIP");

            List<ExtractionProductColumnVO> columns = Lists.newArrayList();

            // TR.YEAR
            {
                ExtractionProductColumnVO column = new ExtractionProductColumnVO();
                column.setLabel("year");
                column.setName("year");
                column.setColumnName("year");

                column.setValues(ImmutableList.of("2016", "2017"));
                columns.add(column);
            }

            // TR.PROJECT
            {
                ExtractionProductColumnVO column = new ExtractionProductColumnVO();
                column.setLabel("project");
                column.setName("project");
                column.setColumnName("project");

                column.setValues(ImmutableList.of("CTY-PRJ"));
                columns.add(column);
            }

            tables.add(table);
        }

        // HH
        {
            ExtractionProductTableVO table = new ExtractionProductTableVO();
            table.setLabel("HH");
            table.setName("Station");
            table.setTableName("P01_RDB_STATION");
            tables.add(table);
        }

        source.setTables(tables);

        // Save
        ExtractionProductVO target = dao.save(source);
        Assert.assertNotNull(target);
        Assert.assertNotNull(target.getId());

        // Reload
        ExtractionProductVO reloadTarget = dao.getByLabel(source.getLabel());
        Assert.assertNotNull(reloadTarget);
        Assert.assertNotNull(reloadTarget.getId());

    }

}
