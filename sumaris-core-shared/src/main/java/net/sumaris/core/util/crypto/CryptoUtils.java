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


import com.lambdaworks.codec.Base64;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import org.abstractj.kalium.crypto.Util;

import javax.annotation.Nullable;
import java.nio.charset.Charset;

public class CryptoUtils extends Util {
	
	public static final Charset CHARSET_UTF8 = Files.CHARSET_UTF8;
	public static final Charset CHARSET_ASCII = Charset.forName("US-ASCII");
	
	public static byte[] zeros(int n) {
        return new byte[n];
    }
	
	public static byte[] copyEnsureLength(byte[] source, int length) {
		byte[] result = zeros(length);
		if (source.length > length) {
			System.arraycopy(source, 0, result, 0, length);
		}
		else {
			System.arraycopy(source, 0, result, 0, source.length);
		}
        return result;
    }

	protected static Charset initCharset(String charsetName) {
		Charset result = Charset.forName(charsetName);
		if (result == null) {
			throw new SumarisTechnicalException("Could not load charset: " + charsetName);
		}
		return result;
	}

	public static byte[] decodeUTF8(String string) {
		return string.getBytes(CHARSET_UTF8);
	}

	public static String encodeUTF8(byte[] bytes) {
		return new String(bytes, CHARSET_UTF8);
	}
	
	public static byte[] decodeAscii(String string) {
		return string.getBytes(CHARSET_ASCII);
	}
	

	public static byte[] decodeBase64(String data) {
		return Base64.decode(data.toCharArray());
	}
	
	public static String encodeBase64(byte[] data) {
		return new String(Base64.encode(data));
	}
	
	public static byte[] decodeBase58(String data) {
		try {
			return Base58.decode(data);
		} catch (AddressFormatException e) {
			throw new SumarisTechnicalException("Could decode from base 58: " + e.getMessage());
		}
	}
	
	public static String encodeBase58(byte[] data) {
		return Base58.encode(data);
	}

	public static boolean isValidPubkey(String pubkey) {

		// TODO: check if 43 or 44 characters ?
		return StringUtils.length(pubkey) >= 10;
	}

	public static String minifyPubkey(@Nullable String pubkey) {
		if (pubkey == null || pubkey.length() < 8) return "null";
		return pubkey.substring(0, 8);
	}
}
