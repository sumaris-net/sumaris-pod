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

    private final CryptoService service = new CryptoServiceImpl();

    @Test
    public void getPubkey() {
        // Basic (ascii) password
        {
            String pubkey = service.getPubkey("abc", "def");
            Assert.assertEquals("G2CBgZBPLe6FSFUgpx2Jf1Aqsgta6iib3vmDRA1yLiqU", pubkey);
        }

        // Password with '_'
        {
            String pubkey = service.getPubkey("abc_", "def_");
            Assert.assertEquals("H8TUJWwvntrJpWFeouQyAFx6oEQRauE9mpiPCvvsUpZT", pubkey);
        }

        // Password with '＿'
        {
            String pubkey = service.getPubkey("abc＿", "def＿");
            Assert.assertEquals("7UngWTjRr2DqmqoXRoF9Ywt6F1jomNjc3UoAM2Jv8cYL", pubkey);
        }

        // UTF8 characters (see issue sumaris-app#626)
        String specialCharacters1 = "&é\"'(-è_çà)=$*ù!:;,<";
        String specialCharacters2 = "~#{[|`\\^@]}£µ%§/.?>";
        {
            String pubkey = service.getPubkey(specialCharacters1, specialCharacters2);
            Assert.assertEquals("6uHuoNJ5LMh2P1AKMoSD8HsH6UEEjXqDPyisdufirR5Q", pubkey);
        }
        {
            String pubkey = service.getPubkey(specialCharacters2, specialCharacters1);
            Assert.assertEquals("DhTeDxvVoCAVp3vykkbrDqYezrPorhP9e34djVVWKGds", pubkey);
        }

    }




}
