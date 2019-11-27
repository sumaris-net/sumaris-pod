package net.sumaris.core.extraction.dao.technical;

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

import net.sumaris.core.util.Dates;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class DaosTest {

    @Test
    public void getSqlToDate() {

        Date date = Dates.getFirstDayOfYear(2019);
        String sql = Daos.getSqlToDate(date);
        Assert.assertNotNull(sql);
        Assert.assertEquals("TO_DATE('2019-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS')", sql);
    }
}
