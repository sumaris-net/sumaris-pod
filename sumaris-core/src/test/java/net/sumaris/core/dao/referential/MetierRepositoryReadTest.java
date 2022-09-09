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

package net.sumaris.core.dao.referential;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.filter.MetierFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Slf4j
public class MetierRepositoryReadTest extends AbstractDaoTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    protected MetierRepository metierRepository;

    @Autowired
    protected ReferentialDao referentialDao;

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void findByTextFilter() throws ParseException {
        MetierFilterVO filter = new MetierFilterVO();
        filter.setStatusIds(new Integer[]{1});
        filter.setSearchText("Crustaceans*");
        //filter.setSearchAttribute("");
        List<MetierVO> metiers = metierRepository.findByFilter(filter, 0, 100, null, null);
        Assert.assertNotNull(metiers);
        Assert.assertTrue(metiers.size() > 0);
    }

    @Test
    public void findByMetierPredocFilter() throws ParseException {
        // With vessel and date
        {
            MetierFilterVO filter = new MetierFilterVO();
            filter.setSearchJoin(TaxonGroup.class.getSimpleName());
            filter.setVesselId(1);
            filter.setStartDate(Dates.parseDate("2010-01-01", "yyyy-MM-dd"));
            filter.setEndDate(Dates.parseDate("2021-02-20", "yyyy-MM-dd"));

            List<MetierVO> metiers = metierRepository.findByFilter(filter, 0, 100, null, null);
            Assert.assertNotNull(metiers);
            Assert.assertEquals(3, metiers.size());
        }

        // With program
        {
            MetierFilterVO filter = new MetierFilterVO();
            filter.setVesselId(1);
            filter.setProgramLabel("SUMARiS");
            filter.setStartDate(Dates.parseDate("2018-01-01", "yyyy-MM-dd"));
            filter.setEndDate(Dates.parseDate("2018-02-20", "yyyy-MM-dd"));

            List<MetierVO> metiers = metierRepository.findByFilter(filter, 0, 100, null, null);
            Assert.assertNotNull(metiers);
            Assert.assertEquals(2, metiers.size());
        }

    }



}
