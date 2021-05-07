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

package net.sumaris.server.http.security;

import lombok.NonNull;
import net.sumaris.core.exception.SumarisTechnicalException;

public enum AuthTokenTypeEnum {
    TOKEN("token"),
    BASIC("basic"),
    BASIC_AND_TOKEN("basic-and-token")
    ;

    private String label;

    AuthTokenTypeEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AuthTokenTypeEnum fromLabel(@NonNull String label) {
        for (AuthTokenTypeEnum item: values()) {
            if (label.equalsIgnoreCase(item.label)) {
                return item;
            }
        }
        throw new SumarisTechnicalException(String.format("Unknown authentication type '%s'", label));
    }

    public static AuthTokenTypeEnum[] fromLabels(@NonNull String... labels) {
        AuthTokenTypeEnum[] result = new AuthTokenTypeEnum[labels.length];
        for (int i = 0; i < labels.length; i++) {
            result[i] = fromLabel(labels[0]);
        }
        return result;
    }
}
