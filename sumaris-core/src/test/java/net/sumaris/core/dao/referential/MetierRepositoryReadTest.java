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

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.filter.MetierFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

public class MetierRepositoryReadTest extends AbstractDaoTest{

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(MetierRepositoryReadTest.class);

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
    public void findByMetierFilter() throws ParseException {
        // With vessel and date
        {
            MetierFilterVO filter = new MetierFilterVO();
            filter.setVesselId(1);
            filter.setDate(Dates.parseDate("2018-05-15", "yyyy-MM-dd"));

            List<MetierVO> metiers = metierRepository.findByFilter(filter, 0, 100, null, null);
            Assert.assertNotNull(metiers);
            Assert.assertEquals(1, metiers.size());
        }

        // With program
        {
            MetierFilterVO filter = new MetierFilterVO();
            filter.setVesselId(1);
            filter.setProgramLabel("SUMARiS");
            filter.setDate(Dates.parseDate("2018-05-15", "yyyy-MM-dd"));

            List<MetierVO> metiers = metierRepository.findByFilter(filter, 0, 100, null, null);
            Assert.assertNotNull(metiers);
            Assert.assertEquals(1, metiers.size());
        }

    }



}
