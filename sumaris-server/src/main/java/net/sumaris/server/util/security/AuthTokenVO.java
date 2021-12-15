/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.server.util.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.ParseException;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenVO {

    private String username;
    private String pubkey;
    private String challenge;
    private String signature;

    public String toString() {
        return String.format("%s:%s|%s", pubkey, challenge, signature);
    }

    public String asToken() {
        return toString();
    }

    public static AuthTokenVO parse(String token) throws ParseException {
        int index1 = token.indexOf(':');
        if (index1 == -1) {
            throw new ParseException("Invalid token. Expected format is: <pubkey>:<challenge>|<signature>", 0);
        }
        int index2 = token.indexOf('|', index1);
        if (index2 == -1) {
            throw new ParseException("Invalid token. Expected format is: <pubkey>:<challenge>|<signature>", index1);
        }
        return AuthTokenVO.builder()
            .pubkey(token.substring(0, index1))
            .challenge(token.substring(index1+1, index2))
            .signature(token.substring(index2+1))
            .build();
    }

}
