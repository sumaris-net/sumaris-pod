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
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class ExtractionProductRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ExtractionProductRepository repository;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete()
    }
    @Test
    public void save() {
        ExtractionProductVO source = new ExtractionProductVO();
        source.setLabel("RDB-" + System.currentTimeMillis());
        source.setName("Test RDB product");
        source.setStatusId(StatusEnum.ENABLE.getId());
        source.setIsSpatial(false);
        source.setProcessingFrequencyId(ProcessingFrequencyEnum.DAILY.getId());

        DepartmentVO dep = new DepartmentVO();
        dep.setId(fixtures.getDepartmentId(0));
        source.setRecorderDepartment(dep);

        List<ExtractionTableVO> tables = Lists.newArrayList();
        // TR
        {
            ExtractionTableVO table = new ExtractionTableVO();
            table.setLabel("TR");
            table.setName("Trip");
            table.setTableName("P01_RDB_TRIP");

            List<ExtractionTableColumnVO> columns = Lists.newArrayList();

            // TR.YEAR
            {
                ExtractionTableColumnVO column = new ExtractionTableColumnVO();
                column.setLabel("year");
                column.setName("year");
                column.setColumnName("year");

                column.setValues(ImmutableList.of("2016", "2017"));
                columns.add(column);
            }

            // TR.PROJECT
            {
                ExtractionTableColumnVO column = new ExtractionTableColumnVO();
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
            ExtractionTableVO table = new ExtractionTableVO();
            table.setLabel("HH");
            table.setName("Station");
            table.setTableName("P01_RDB_STATION");
            tables.add(table);
        }
        source.setTables(tables);

        // Stratum
        List<AggregationStrataVO> stratum = Lists.newArrayList();
        {
            AggregationStrataVO strata = new AggregationStrataVO();
            strata.setLabel("default");
            strata.setSheetName("TR");
            stratum.add(strata);
        }
        source.setStratum(stratum);

        // Save
        ExtractionProductVO target = repository.save(source);
        Assert.assertNotNull(target);
        Assert.assertNotNull(target.getId());

        // Reload
        ExtractionProductVO reloadTarget = repository.getByLabel(source.getLabel());
        Assert.assertNotNull(reloadTarget);
        Assert.assertNotNull(reloadTarget.getId());

        // Check stratum
        Assert.assertNotNull(reloadTarget.getStratum());
        Assert.assertEquals(1, reloadTarget.getStratum().size());
    }

}
