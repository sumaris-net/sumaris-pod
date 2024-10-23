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

package net.sumaris.core.vo.referential.pmfm;

import lombok.NonNull;
import net.sumaris.core.model.referential.pmfm.Pmfm;

import javax.annotation.Nullable;

/**
 * Same as ParameterValueType, but with INTEGER (when the PMFM has no decimal number)
 */
public enum PmfmValueType {

    INTEGER,
    DOUBLE,
    STRING,
    QUALITATIVE_VALUE,
    BOOLEAN,
    DATE
    ;

    public static PmfmValueType fromPmfm(Pmfm source) {

        ParameterValueType paramType = ParameterValueType.fromParameter(source.getParameter());

        // If not decimal, use INTEGER
        if (paramType == ParameterValueType.DOUBLE
                && source.getMaximumNumberDecimals() != null && source.getMaximumNumberDecimals() <= 0) {
            return  PmfmValueType.INTEGER;
        }

        return fromString(paramType.name());
    }


    public static PmfmValueType fromPmfm(@Nullable PmfmVO pmfm) {
        if (pmfm == null) return null;
        return fromString(pmfm.getType());
    }

    /**
     *
     * @param name
     * @return
     * @throws IllegalArgumentException
     */
    public static PmfmValueType fromString(@NonNull String name) {
        return PmfmValueType.valueOf(name.toUpperCase());
    }
}
