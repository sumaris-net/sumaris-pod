package net.sumaris.core.service.technical.crypto;

/*
 * #%L
 * Duniter4j :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import net.sumaris.core.util.crypto.KeyPair;

/**
 * Crypto services (sign...)
 * Created by eis on 10/01/15.
 */
public interface CryptoService {

    /**
     * generate a crypto seed, using default N,r,p parameters (4096,16,1)
     * @param salt
     * @param password
     * @return
     */
    byte[] getSeed(String salt, String password);

    byte[] getSeed(String salt, String password, int N, int r, int p);

    /**
     * Returns pubkey from salt and password.
     *
     * @param salt
     * @param password
     * @return
     */
    String getPubkey(String salt, String password);

    /**
     * Returns a new signing key pair generated from salt and password.
     * The salt and password must contain enough entropy to be secure.
     *
     * @param salt
     * @param password
     * @return
     */
    KeyPair getKeyPair(String salt, String password);

    /**
     * Returns a new signing key pair generated from salt and password.
     * The salt and password must contain enough entropy to be secure.
     *
     * @param seed
     * @return
     */
    KeyPair getKeyPairFromSeed(byte[] seed);

    KeyPair getRandomKeypair();

    String sign(String message, byte[] secretKey);

    String sign(String message, String secretKey);

    String box(String message, byte[] nonce, String senderSignSk, String receiverSignPk);

    String box(String message, byte[] nonce, byte[] senderSignSk, byte[] receiverSignPk);

    byte[] getBoxRandomNonce();

    String openBox(String cypherText, String nonce, String senderSignPk, String receiverSignSk);

    String openBox(String cypherText, byte[] nonce, byte[] senderSignPk, byte[] receiverSignSk);

    boolean verify(String message, String signature, String publicKey);

    /**
     * Do a SHA256 then a hexa convert
     * @param message
     * @return
     */
    String hash(String message);

}
