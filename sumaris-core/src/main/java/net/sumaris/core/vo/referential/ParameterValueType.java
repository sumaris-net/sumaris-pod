package net.sumaris.core.vo.referential;

import net.sumaris.core.model.referential.Parameter;
import net.sumaris.core.model.referential.Pmfm;

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
