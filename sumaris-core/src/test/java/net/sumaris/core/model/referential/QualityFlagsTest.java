package net.sumaris.core.model.referential;

/*-
 * #%L
 * SUMARiS:: Core
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

import net.sumaris.core.util.crypto.MD5Util;
import org.junit.Assert;
import org.junit.Test;

public class QualityFlagsTest {

    @Test
    public void worst() {

        // Test using enums
        {
            QualityFlagEnum worstQualityFlags = QualityFlags.worst(QualityFlagEnum.NOT_QUALIFIED, QualityFlagEnum.BAD, QualityFlagEnum.FIXED);
            Assert.assertEquals(QualityFlagEnum.BAD, worstQualityFlags);
        }

        // Test using ids
        {
            Integer worstQualityFlags = QualityFlags.worst(QualityFlagEnum.NOT_QUALIFIED.getId(), QualityFlagEnum.BAD.getId(), QualityFlagEnum.FIXED.getId());
            Assert.assertEquals(QualityFlagEnum.BAD.getId(), worstQualityFlags);

            worstQualityFlags = QualityFlags.worst(QualityFlagEnum.NOT_QUALIFIED.getId(), QualityFlagEnum.GOOD.getId(), QualityFlagEnum.FIXED.getId());
            Assert.assertEquals(QualityFlagEnum.FIXED.getId(), worstQualityFlags);
        }
    }

}
