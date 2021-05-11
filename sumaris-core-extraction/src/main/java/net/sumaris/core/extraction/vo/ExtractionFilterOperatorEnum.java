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
import net.sumaris.core.exception.SumarisTechnicalException;

import java.util.Arrays;

public enum ExtractionFilterOperatorEnum {

    IN("IN", "NOT IN"),
    NOT_IN("NOT IN", "IN"),
    EQUALS("=", "!="),
    NOT_EQUALS("!=", "="),
    GREATER_THAN(">", "<="),
    GREATER_THAN_OR_EQUALS(">=", "<"),
    LESS_THAN("<", ">="),
    LESS_THAN_OR_EQUALS("<=", ">"),
    BETWEEN("BETWEEN", "NOT BETWEEN"),
    NOT_BETWEEN("NOT BETWEEN", "BETWEEN"), // WARN not exists in SQL. Should be translated
    NULL("NULL", "NOT NULL"),
    NOT_NULL("NOT NULL", "NULL") // WARN not exists in SQL. Should be translated
    ;

    private String symbol;
    private String inverseSymbol;

    ExtractionFilterOperatorEnum(String symbol, String inverseSymbol) {
        this.symbol = symbol;
        this.inverseSymbol = inverseSymbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static ExtractionFilterOperatorEnum fromSymbol(String operator) {
        Preconditions.checkNotNull(operator);
        return Arrays.stream(values())
                .filter(op -> op.symbol.equalsIgnoreCase(operator))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid operator's symbol '%s'", operator)));
    }

    public ExtractionFilterOperatorEnum inverse() {
        return fromSymbol(inverseSymbol);
    }

}
