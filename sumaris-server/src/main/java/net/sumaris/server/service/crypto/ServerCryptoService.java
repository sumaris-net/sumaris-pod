package net.sumaris.server.service.crypto;

/*-
 * #%L
 * SUMARiS:: Server
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

import net.sumaris.core.service.technical.crypto.CryptoService;
import net.sumaris.core.util.crypto.KeyPair;

public interface ServerCryptoService extends  CryptoService {

    /**
     * Get the server public key
     * @return
     */
    String getServerPubkey();

    /**
     * Get the server keypair
     * @return
     */
    KeyPair getServerKeypair();

    /**
     * Sign a message, using the server keypair
     * @param message
     * @return
     */
    String sign(String message);

}
