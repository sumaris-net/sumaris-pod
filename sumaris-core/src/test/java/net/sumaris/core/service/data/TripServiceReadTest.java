package net.sumaris.core.service.data;

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

import net.sumaris.core.util.Dates;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TripServiceReadTest extends AbstractServiceTest{

    @Autowired
    private TripService service;

    @Test
    public void findTrips() throws ParseException {
        TripFilterVO filter = new TripFilterVO();

        Date tripDay = new SimpleDateFormat("yyyy-MM-dd").parse("2018-03-03");
        filter.setStartDate(Dates.resetTime(tripDay));
        filter.setEndDate(Dates.lastSecondOfTheDay(tripDay));

        List<TripVO> trips = service.findByFilter(filter, 0, 100);
        Assert.assertNotNull(trips);
        Assert.assertTrue(trips.size() > 0);
    }

}
