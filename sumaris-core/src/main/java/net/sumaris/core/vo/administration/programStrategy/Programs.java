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

package net.sumaris.core.vo.administration.programStrategy;

import com.google.common.base.Splitter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Helper class for program
 */
@Slf4j
public class Programs {

    protected Programs(){
        // Helper class
    }

    public static boolean getPropertyAsBoolean(@NonNull ProgramVO source, ProgramPropertyEnum property) {
        return MapUtils.getBooleanValue(source.getProperties(), property.getKey(), Boolean.getBoolean(property.getDefaultValue()));
    }

    public static String getProperty(@NonNull ProgramVO source, ProgramPropertyEnum property) {
        return MapUtils.getString(source.getProperties(), property.getKey(), property.getDefaultValue());
    }

    public static Integer getPropertyAsInteger(@NonNull ProgramVO source, ProgramPropertyEnum property) {
        return MapUtils.getInteger(source.getProperties(), property.getKey(), property.getDefaultValue() != null ? Integer.parseInt(property.getDefaultValue()) : null);
    }

    public static Integer[] getPropertyAsIntegers(@NonNull ProgramVO source, ProgramPropertyEnum property) {
        String strValue = MapUtils.getString(source.getProperties(), property.getKey(), property.getDefaultValue());
        if (StringUtils.isBlank(strValue)) return null;
        return Arrays.stream(strValue.split(",")).filter(StringUtils::isNotBlank).map((value) -> {
            try {
                return Integer.valueOf(value);
            } catch(NumberFormatException e) {
                log.error("Invalid integer value: {}", value, e);
                return null;
            }
        }).filter(Objects::nonNull).toArray(Integer[]::new);
    }
}
