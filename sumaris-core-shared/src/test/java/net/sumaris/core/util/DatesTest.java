package net.sumaris.core.util;

/*-
 * #%L
 * SUMARiS:: Core shared
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

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class DatesTest {

    @Test
    public void resetTime() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone paris = TimeZone.getTimeZone("Europe/Paris");
        TimeZone mahe = TimeZone.getTimeZone("Indian/Mahe");

        // 12h at Europe/Paris => 0h à Europe/Paris
        {
            Date date = Dates.fromISODateTimeString("2022-03-21T23:00:00.000+01:00");
            Date result = Dates.resetTime(date, paris);
            Assert.assertEquals("2022-03-21T00:00:00.000+01:00", Dates.toISODateTimeString(result, paris));
        }
        // 0h UTC => 0h à Europe/Paris
        {
            Date date = Dates.fromISODateTimeString("2022-03-21T00:00:00.000Z");
            Date result = Dates.resetTime(date, paris);
            Assert.assertEquals("2022-03-21T00:00:00.000+01:00", Dates.toISODateTimeString(result, paris));
        }

        // 0h at UTC => 20h UTC, BUT the previous day
        {
            Date date = Dates.fromISODateTimeString("2022-03-21T00:00:00.000Z");
            Date result = Dates.resetTime(date, TimeZone.getTimeZone("Indian/Mahe"));
            Assert.assertEquals("2022-03-20T20:00:00.000Z", Dates.toISODateTimeString(result, utc));
        }

        // 1h at Europe/Paris => 0h Mahe
        {
            Date date = Dates.fromISODateTimeString("2022-03-14T22:00:00.000Z");
            Date result = Dates.resetTime(date, mahe);
            Assert.assertEquals("2022-03-15T00:00:00.000+04:00", Dates.toISODateTimeString(result, mahe));
            Assert.assertEquals("2022-03-14T20:00:00.000Z", Dates.toISODateTimeString(result, utc));
        }
    }
}
