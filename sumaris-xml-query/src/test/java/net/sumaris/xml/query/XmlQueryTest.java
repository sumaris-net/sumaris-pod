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

package net.sumaris.xml.query;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Ignore
public class XmlQueryTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Resource
    MyXmlQuery xmlQuery;

    @Test
    public void generateSqlRwSpeciesList() throws Exception {

        //xmlQuery.setQuery("classpath:xmlQuery/apase/v1_0/createRawSpeciesListDenormalizeTable.xml");

        // Inject
        xmlQuery.injectQuery("classpath:xmlQuery/apase/v1_0/injectionRawSpeciesListTable.xml", "afterSpeciesInjection");

        xmlQuery.setGroup("excludeInvalidStation", false);
        xmlQuery.bind("catchCategoryPmfmId", "90");
        xmlQuery.bind("subsamplingCategoryPmfmId", "176");
        String sql = xmlQuery.getSQLQueryAsString();
        System.out.println(sql);
    }
}
