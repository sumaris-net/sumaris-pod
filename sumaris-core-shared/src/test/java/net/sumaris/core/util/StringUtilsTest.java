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

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class StringUtilsTest {

    @Test
    public void underscoreToChangeCase() {
        //Assert.assertEquals("expectedColumnName", StringUtils.underscoreToChangeCase("EXPECTED_COLUMN_NAME"));

        Assert.assertEquals("aBC", StringUtils.underscoreToChangeCase("A_B_C"));

        Assert.assertEquals("aB", StringUtils.underscoreToChangeCase("A_B_"));

        Assert.assertEquals("abCd", StringUtils.underscoreToChangeCase("_AB_CD"));
    }
}
