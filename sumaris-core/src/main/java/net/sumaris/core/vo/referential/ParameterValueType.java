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
import net.sumaris.core.model.referential.pmfm.Pmfm;

import java.util.Objects;

public enum ParameterValueType {

    INTEGER,
    DOUBLE,
    STRING,
    QUALITATIVE_VALUE,
    BOOLEAN,
    DATE
    ;

    public static ParameterValueType fromPmfm(Pmfm source) {

        Parameter parameter = source.getParameter();

        // Parameter Type
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
        else if (source.getMaximumNumberDecimals() == null || source.getMaximumNumberDecimals() > 0) {
            return  ParameterValueType.DOUBLE;
        }

        else {
            return ParameterValueType.INTEGER;
        }
    }

    public static ParameterValueType fromPmfm(PmfmVO pmfm) {
        return pmfm != null ? fromString(pmfm.getType()) : null;
    }

    public static ParameterValueType fromString(String name) {
        switch (name.toUpperCase()) {
            case "BOOLEAN":
                return ParameterValueType.BOOLEAN;
            case "QUALITATIVE_VALUE":
                return ParameterValueType.QUALITATIVE_VALUE;
            case "STRING":
                return ParameterValueType.STRING;
            case "DATE":
                return ParameterValueType.DATE;
            case "INTEGER":
                return ParameterValueType.INTEGER;
            case "DOUBLE":
                return ParameterValueType.DOUBLE;
            default:
                return null;
        }
    }
}
