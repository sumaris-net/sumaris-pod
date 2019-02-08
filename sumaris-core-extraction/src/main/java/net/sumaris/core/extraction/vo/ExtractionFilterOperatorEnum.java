package net.sumaris.core.extraction.vo;

/*-
 * #%L
 * SUMARiS:: Core
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

import com.google.common.base.Preconditions;

public enum ExtractionFilterOperatorEnum {

    IN,
    NOT_IN,
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN,
    LESS_THAN_OR_EQUALS,
    BETWEEN;

    public static ExtractionFilterOperatorEnum fromSymbol(String operator) {
        Preconditions.checkNotNull(operator);
        switch (operator.toUpperCase()) {
            case "=": return EQUALS;
            case "!=": return NOT_EQUALS;
            case ">": return GREATER_THAN;
            case ">=": return GREATER_THAN_OR_EQUALS;
            case "<": return LESS_THAN;
            case "<=": return LESS_THAN_OR_EQUALS;
            case "IN": return IN;
            case "NOT IN": return NOT_IN;
            case "BETWEEN": return BETWEEN;
            default:
                throw new IllegalArgumentException("Unknown operation symbol");
        }

    }
}
