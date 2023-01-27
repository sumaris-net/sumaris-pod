package net.sumaris.core.util.crypto;

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

import org.junit.Assert;
import org.junit.Test;

public class MD5UtilTest {

    @Test
    public void md5Hex() {
        String emailMd5 = MD5Util.md5Hex("demo@sumaris.net");
        Assert.assertEquals("2c4f83386923812f818a88ede88ce334", emailMd5);

        emailMd5 = MD5Util.md5Hex("obs@sumaris.net");
        Assert.assertEquals("67655fe01f8693efeea27497939394d7", emailMd5);

        emailMd5 = MD5Util.md5Hex("disable@sumaris.net");
        Assert.assertEquals("cf8bdc002a99cbc2a5f1a891de7194cf", emailMd5);

        emailMd5 = MD5Util.md5Hex("guest@sumaris.net");
        Assert.assertEquals("251dd0ef9a7744e87ea321a87d52d545", emailMd5);

    }




}
