package net.sumaris.core.util.crypto;

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


import com.lambdaworks.crypto.SCrypt;
import jnr.ffi.byref.LongLongByReference;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.shared.exception.ErrorCodes;
import org.abstractj.kalium.crypto.Util;

import java.security.GeneralSecurityException;

import static org.abstractj.kalium.NaCl.Sodium.*;
import static org.abstractj.kalium.NaCl.sodium;
import static org.abstractj.kalium.crypto.Util.*;


public class SecretBox {

	// Length of the key
	private static int SEED_LENGTH = 32;
	private static int SCRYPT_PARAMS_N = 4096;
	private static int SCRYPT_PARAMS_r = 16;
	private static int SCRYPT_PARAMS_p = 1;

	private final String pubKey;
    private final byte[] secretKey;
    private final byte[] seed;

	public SecretBox(String salt, String password) {
		this(computeSeedFromSaltAndPassword(salt, password));
	}
	
	public SecretBox(byte[] seed) {
		checkLength(seed, SEED_LENGTH);
		this.seed = seed;
        this.secretKey = CryptoUtils.zeros(CRYPTO_SIGN_ED25519_SECRETKEYBYTES);
        byte[] publicKey = CryptoUtils.zeros(CRYPTO_SIGN_ED25519_PUBLICKEYBYTES);
        isValid(sodium().crypto_sign_ed25519_seed_keypair(publicKey, secretKey, seed),
                "Failed to generate a key pair");
        this.pubKey = Base58.encode(publicKey);
	}
	
	/**
	 * Retrun the public key, encode in Base58
	 * @return
	 */
	public String getPublicKey() {
		return pubKey;
	}
	
	/**
	 * Return the secret key, encode in Base58
	 * @return
	 */
	public String getSecretKey() {
		return Base58.encode(secretKey);
	}
	
	public String sign(String message) {
		byte[] messageBinary = CryptoUtils.decodeUTF8(message);
		return CryptoUtils.encodeBase64(
				sign(messageBinary)
				);
	}
	
	public byte[] sign(byte[] message) {
        byte[] signature = Util.prependZeros(CRYPTO_SIGN_ED25519_BYTES, message);
        LongLongByReference bufferLen = new LongLongByReference(0);
        sodium().crypto_sign_ed25519(signature, bufferLen, message, message.length, secretKey);
        signature = Util.slice(signature, 0, CRYPTO_SIGN_ED25519_BYTES);
        
        checkLength(signature, CRYPTO_SIGN_ED25519_BYTES);
        return signature;
    }


    public byte[] encrypt(byte[] nonce, byte[] message) {
        checkLength(nonce, CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES);
        byte[] msg = Util.prependZeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES, message);
        byte[] ct = Util.zeros(msg.length);
        isValid(sodium().crypto_secretbox_xsalsa20poly1305(ct, msg, msg.length,
                nonce, seed), "Encryption failed");
        return removeZeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES, ct);
    }

    public byte[] decrypt(byte[] nonce, byte[] ciphertext) {
        checkLength(nonce, CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES);
        byte[] ct = Util.prependZeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_BOXZEROBYTES, ciphertext);
        byte[] message = Util.zeros(ct.length);
        isValid(sodium().crypto_secretbox_xsalsa20poly1305_open(message, ct,
                ct.length, nonce, seed), "Decryption failed. Ciphertext failed verification");
        return removeZeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES, message);
    }
    
	/* -- Internal methods -- */
	
	public static byte[] computeSeedFromSaltAndPassword(String salt, String password) {
		try {
			byte[] seed = SCrypt.scrypt(
					CryptoUtils.decodeAscii(password),
					CryptoUtils.decodeAscii(salt),
					SCRYPT_PARAMS_N, SCRYPT_PARAMS_r,
					SCRYPT_PARAMS_p, SEED_LENGTH);
			return seed;
		} catch (GeneralSecurityException e) {
			throw new SumarisTechnicalException(
					"Unable to salt password, using Scrypt library", e);
		}
	}
	
	
}
