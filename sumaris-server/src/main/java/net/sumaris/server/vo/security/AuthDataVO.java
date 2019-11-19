package net.sumaris.server.vo.security;

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

import lombok.Data;

import java.text.ParseException;

@Data
public class AuthDataVO {
    private String pubkey;
    private String challenge;
    private String signature;

    public AuthDataVO(){
    }

    public AuthDataVO(String pubkey, String challenge, String signature) {
        this.pubkey = pubkey;
        this.challenge = challenge;
        this.signature = signature;
    }

    public String toString() {
        return String.format("%s:%s|%s", pubkey, challenge, signature);
    }

    public String asToken() {
        return toString();
    }

    public static AuthDataVO parse(String token) throws ParseException {
        int index1 = token.indexOf(':');
        if (index1 == -1) {
            throw new ParseException("Invalid token. Expected format is: <pubkey>:<challenge>|<signature>", 0);
        }
        int index2 = token.indexOf('|', index1);
        if (index2 == -1) {
            throw new ParseException("Invalid token. Expected format is: <pubkey>:<challenge>|<signature>", index1);
        }
        return new AuthDataVO(
                token.substring(0, index1),
                token.substring(index1+1, index2),
                token.substring(index2+1));
    }

}
