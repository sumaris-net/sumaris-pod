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

package net.sumaris.core.model.referential;

import lombok.NonNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public abstract class QualityFlags {

    protected QualityFlags(){
        // helper class
    }

    public static boolean isInvalid(int qualityFlagId) {
        return isInvalid(QualityFlagEnum.valueOf(qualityFlagId));
    }

    public static boolean isInvalid(@NonNull QualityFlagEnum qualityFlag) {
        switch (qualityFlag) {
            case BAD:
            case MISSING:
            case NOT_COMPLETED:
                return true;
            default:
                return false;
        }
    }

    public static boolean isValid(int qualityFlagId) {
        return isValid(QualityFlagEnum.valueOf(qualityFlagId));
    }

    public static boolean isValid(@NonNull QualityFlagEnum qualityFlag) {
        return !isInvalid(qualityFlag);
    }

    public static Integer worst(Integer... qualityFlags) {
        return Arrays.stream(qualityFlags)
            .filter(Objects::nonNull)
            // Sort (invalid first, with a negative id)
            .sorted(Comparator.comparingInt(qualityFlagId -> isInvalid(qualityFlagId) ? -1 * qualityFlagId : (10 - qualityFlagId)))
            .findFirst()
            .orElse(null);
    }

    public static QualityFlagEnum worst(QualityFlagEnum... qualityFlags) {
        return Arrays.stream(qualityFlags)
            .filter(Objects::nonNull)
            // Sort (invalid first, with a negative id)
            .sorted(Comparator.comparingInt(qualityFlag -> isInvalid(qualityFlag) ? -1 * qualityFlag.getId() : (10 - qualityFlag.getId())))
            .findFirst()
            .orElse(null);
    }
}
