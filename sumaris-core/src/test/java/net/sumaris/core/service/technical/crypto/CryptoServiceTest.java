package net.sumaris.core.service.technical.crypto;

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

public class CryptoServiceTest {

    private CryptoService service = new CryptoServiceImpl();

    @Test
    public void getPubkey() {
        // Basic (ascii) password
        {
            String pubkey = service.getPubkey("abc", "def");
            Assert.assertEquals("G2CBgZBPLe6FSFUgpx2Jf1Aqsgta6iib3vmDRA1yLiqU", pubkey);
        }

        // UTF8 password (issue sumaris-app#626)
        {
            String pubkey = service.getPubkey("&é\"'(-è_çà)=$*ù!:;,<", "~#{[|`\\^@]}£µ%§/.?>");
            Assert.assertEquals("6uHuoNJ5LMh2P1AKMoSD8HsH6UEEjXqDPyisdufirR5Q", pubkey);
        }
    }




}
