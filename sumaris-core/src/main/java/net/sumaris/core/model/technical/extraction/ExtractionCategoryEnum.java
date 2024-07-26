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

package net.sumaris.core.model.technical.extraction;

import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public enum ExtractionCategoryEnum {

    /**
     * Data from an existing product
     */
    PRODUCT,

    /**
     * Raw data extraction
     */
    LIVE;

    public static Optional<ExtractionCategoryEnum> fromString(@Nullable String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.toUpperCase()));
        }
        catch(IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static ExtractionCategoryEnum valueOfIgnoreCase(@NonNull String value) {
        return fromString(value.toUpperCase()).orElseThrow(IllegalArgumentException::new);
    }
}
