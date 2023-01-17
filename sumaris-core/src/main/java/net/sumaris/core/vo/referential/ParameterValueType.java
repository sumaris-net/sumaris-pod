package net.sumaris.core.vo.referential;

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

import net.sumaris.core.model.referential.pmfm.Parameter;

import java.util.Objects;

public enum ParameterValueType {

    DOUBLE,
    STRING,
    QUALITATIVE_VALUE,
    BOOLEAN,
    DATE
    ;

    public static ParameterValueType fromParameter(Parameter parameter) {

        if (Objects.equals(Boolean.TRUE, parameter.getIsBoolean())) {
            return BOOLEAN;
        }
        else if (Objects.equals(Boolean.TRUE, parameter.getIsQualitative())) {
            return QUALITATIVE_VALUE;
        }
        else if (Objects.equals(Boolean.TRUE, parameter.getIsAlphanumeric())) {
            return ParameterValueType.STRING;
        }
        else if (Objects.equals(Boolean.TRUE, parameter.getIsDate())) {
            return ParameterValueType.DATE;
        }
        else {
            return ParameterValueType.DOUBLE;
        }
    }

    public static ParameterValueType fromParameter(ParameterVO parameter) {
        return parameter != null ? fromString(parameter.getType()) : null;
    }

    public static ParameterValueType fromString(String name) {
        return switch (name.toUpperCase()) {
            case "BOOLEAN" -> ParameterValueType.BOOLEAN;
            case "QUALITATIVE_VALUE" -> ParameterValueType.QUALITATIVE_VALUE;
            case "STRING" -> ParameterValueType.STRING;
            case "DATE" -> ParameterValueType.DATE;
            case "DOUBLE" -> ParameterValueType.DOUBLE;
            default -> null;
        };
    }
}
