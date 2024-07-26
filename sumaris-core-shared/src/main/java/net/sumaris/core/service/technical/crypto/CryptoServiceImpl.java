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


import com.lambdaworks.crypto.SCrypt;
import jnr.ffi.byref.LongLongByReference;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.core.util.crypto.KeyPair;
import org.abstractj.kalium.NaCl;
import org.abstractj.kalium.NaCl.Sodium;
import org.abstractj.kalium.crypto.Util;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;

import static org.abstractj.kalium.NaCl.Sodium.*;
import static org.abstractj.kalium.NaCl.sodium;
import static org.abstractj.kalium.crypto.Util.*;


/**
 * Crypto services (sign...)
 * Created by eis on 10/01/15.
 */
@Component("cryptoService")
public class CryptoServiceImpl implements CryptoService {

    // Length of the seed key (generated deterministically, use to generate the 64 key pair).
    private static int SEED_BYTES = 32;
    // Length of a signature return by crypto_sign
    private static int SIGNATURE_BYTES = 64;
    // Length of a public key
    private static int PUBLICKEY_BYTES = 32;
    // Length of a secret key
    private static int SECRETKEY_BYTES = 64;

    // Scrypt default parameters
    public static int SCRYPT_PARAMS_N = 4096;
    public static int SCRYPT_PARAMS_r = 16;
    public static int SCRYPT_PARAMS_p = 1;

    protected final static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    // Hash
    private static int HASH_BYTES = 256;

    private final Sodium naCl;

    public CryptoServiceImpl() {
        naCl = NaCl.sodium();
    }

    @Override
    public byte[] getSeed(String salt, String password) {
        return getSeed(salt, password, SCRYPT_PARAMS_N, SCRYPT_PARAMS_r, SCRYPT_PARAMS_p);
    }

    @Override
    public byte[] getSeed(String salt, String password, int N, int r, int p) {
        try {
            byte[] seed = SCrypt.scrypt(
                    CryptoUtils.decodeUTF8(password),
                    CryptoUtils.decodeUTF8(salt),
                    N, r, p, SEED_BYTES);
            return seed;
        } catch (GeneralSecurityException e) {
            throw new SumarisTechnicalException(
                    "Unable to salt password, using Scrypt library", e);
        }
    }

    @Override
    public String getPubkey(String salt, String password) {
        byte[] seed = getSeed(salt, password);
        KeyPair keyPair = getKeyPairFromSeed(seed);
        byte[] pubkey = keyPair.getPubKey();
        return CryptoUtils.encodeBase58(pubkey);
    }

    @Override
    public KeyPair getKeyPair(String salt, String password) {
        return getKeyPairFromSeed(getSeed(salt, password));
    }

    @Override
    public KeyPair getKeyPairFromSeed(byte[] seed) {
        byte[] secretKey = CryptoUtils.zeros(SECRETKEY_BYTES);
        byte[] publicKey = CryptoUtils.zeros(PUBLICKEY_BYTES);
        Util.isValid(naCl.crypto_sign_ed25519_seed_keypair(publicKey, secretKey, seed),
                "Failed to generate a key pair");

        return new KeyPair(publicKey, secretKey);
    }

    @Override
    public KeyPair getRandomKeypair() {
        return getKeyPairFromSeed(String.valueOf(System.currentTimeMillis()).getBytes());
    }

    @Override
    public String sign(String message, byte[] secretKey) {
        byte[] messageBinary = CryptoUtils.decodeUTF8(message);
        return CryptoUtils.encodeBase64(
                sign(messageBinary, secretKey)
        );
    }

    @Override
    public String sign(String message, String secretKey) {
        byte[] messageBinary = CryptoUtils.decodeUTF8(message);
        byte[] secretKeyBinary = CryptoUtils.decodeBase58(secretKey);
        return CryptoUtils.encodeBase64(
                sign(messageBinary, secretKeyBinary)
        );
    }

    @Override
    public boolean verify(String message, String signature, String publicKey) {
        byte[] messageBinary = CryptoUtils.decodeUTF8(message);
        byte[] signatureBinary = CryptoUtils.decodeBase64(signature);
        byte[] publicKeyBinary = CryptoUtils.decodeBase58(publicKey);
        return verify(messageBinary, signatureBinary, publicKeyBinary);
    }

    @Override
    public String hash(String message) {
        byte[] hash = new byte[Sodium.CRYPTO_HASH_SHA256_BYTES];
        byte[] messageBinary = CryptoUtils.decodeUTF8(message);
        naCl.crypto_hash_sha256(hash, messageBinary, messageBinary.length);
        return bytesToHex(hash).toUpperCase();
    }

    @Override
    public byte[] getBoxRandomNonce() {
        byte[] nonce = new byte[Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_NONCEBYTES];
        naCl.randombytes(nonce, nonce.length);

        return nonce;
    }

    @Override
    public String box(String message, byte[] nonce, String senderSignSk, String receiverSignPk) {
        byte[] senderSignSkBinary = CryptoUtils.decodeBase58(senderSignSk);
        byte[] receiverSignPkBinary = CryptoUtils.decodeBase58(receiverSignPk);
        return box(message, nonce, senderSignSkBinary, receiverSignPkBinary);
    }

    @Override
    public String box(String message, byte[] nonce, byte[] senderSignSk, byte[] receiverSignPk) {
        checkLength(nonce, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_NONCEBYTES);

        byte[] messageBinary = prependZeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES, CryptoUtils.decodeBase64(message));

        byte[] senderBoxSk = new byte[Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_SECRETKEYBYTES];
        naCl.crypto_sign_ed25519_sk_to_curve25519(senderBoxSk, senderSignSk);

        byte[] receiverBoxPk = new byte[Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES];
        naCl.crypto_sign_ed25519_pk_to_curve25519(receiverBoxPk, receiverSignPk);

        byte[] cypherTextBinary = new byte[messageBinary.length];
        isValid(sodium().crypto_box_curve25519xsalsa20poly1305(cypherTextBinary, messageBinary,
                cypherTextBinary.length, nonce, senderBoxSk, receiverBoxPk), "Encryption failed");
        return CryptoUtils.encodeBase64(removeZeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_BOXZEROBYTES, cypherTextBinary));
    }

    @Override
    public String openBox(String cypherText, String nonce, String senderSignPk, String receiverSignSk) {
        return openBox(cypherText,
                CryptoUtils.decodeBase58(nonce),
                CryptoUtils.decodeBase58(senderSignPk),
                CryptoUtils.decodeBase58(receiverSignSk));
    }

    @Override
    public String openBox(String cypherText, byte[] nonce, byte[] senderSignPk, byte[] receiverSignSk) {
        checkLength(nonce, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_NONCEBYTES);
        byte[] cypherTextBinary = prependZeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_BOXZEROBYTES, CryptoUtils.decodeBase64(cypherText));

        byte[] receiverBoxSk = new byte[Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_SECRETKEYBYTES];
        naCl.crypto_sign_ed25519_sk_to_curve25519(receiverBoxSk, receiverSignSk);

        byte[] senderBoxPk = new byte[Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES];
        naCl.crypto_sign_ed25519_pk_to_curve25519(senderBoxPk, senderSignPk);

        byte[] messageBinary = new byte[cypherTextBinary.length];
        isValid(sodium().crypto_box_curve25519xsalsa20poly1305_open(
                messageBinary, cypherTextBinary, cypherTextBinary.length, nonce, senderBoxPk, receiverBoxSk),
                "Decryption failed. Ciphertext failed verification.");
        return CryptoUtils.encodeUTF8(removeZeros(CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES, messageBinary));
    }

    /* -- Internal methods -- */

    protected byte[] sign(byte[] message, byte[] secretKey) {
        byte[] signature = Util.prependZeros(SIGNATURE_BYTES, message);
        LongLongByReference smLen = new LongLongByReference(0);
        naCl.crypto_sign_ed25519(signature, smLen, message, message.length, secretKey);
        signature = Util.slice(signature, 0, SIGNATURE_BYTES);

        Util.checkLength(signature, SIGNATURE_BYTES);
        return signature;
    }

    protected boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
        byte[] sigAndMsg = new byte[SIGNATURE_BYTES + message.length];
        for (int i = 0; i < SIGNATURE_BYTES; i++) sigAndMsg[i] = signature[i];
        for (int i = 0; i < message.length; i++) sigAndMsg[i+SIGNATURE_BYTES] = message[i];

        byte[] buffer = new byte[SIGNATURE_BYTES + message.length];
        LongLongByReference bufferLength = new LongLongByReference(0);

        int result = naCl.crypto_sign_ed25519_open(buffer, bufferLength, sigAndMsg, sigAndMsg.length, publicKey);
        boolean validSignature = (result == 0);

        return validSignature;
    }

    protected static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_CHARS[v >>> 4];
            hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }

}
