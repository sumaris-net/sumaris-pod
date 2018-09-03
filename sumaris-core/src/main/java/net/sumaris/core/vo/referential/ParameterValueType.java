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
}
