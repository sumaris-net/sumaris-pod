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

package net.sumaris.extraction.core.util;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.specification.data.trip.AggSpecification;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class ExtractionTypes {

    protected ExtractionTypes()  {
        // Helper class
    }


    public static <C extends IExtractionType, T extends IExtractionType> C findOneMatch(C[] availableTypes,
                                                                                        T source) throws IllegalArgumentException {
        return findOneMatch(Arrays.asList(availableTypes), source);
    }

    public static <C extends IExtractionType, T extends IExtractionType> C findOneMatch(Collection<C> availableTypes,
                                                                                        T source) throws IllegalArgumentException {
        return findOneMatch(availableTypes.stream(), source);
    }

    public static <C extends IExtractionType, T extends IExtractionType> C findOneMatch(@NonNull Stream<C> availableTypes,
                                                                                        @NonNull T source) throws IllegalArgumentException {
        Preconditions.checkNotNull(source.getFormat(), "Missing 'format'");

        // Retrieve the extraction type, from list
        final String format = source.getFormat();
        final String version = source.getVersion();
        //final ExtractionCategoryEnum category = source.getCategory();
        return availableTypes
                .filter(aType ->
                        // Same category
                        //(category == null || category.equals(aType.getCategory())) &&
                        // Same format
                         format.equalsIgnoreCase(aType.getFormat())
                        // Same version
                        && (version == null || version.equals(aType.getVersion()))
                )
                .findFirst()
                //.orElseThrow(() -> new IllegalArgumentException(String.format("No match for: {category: %s, format: %s, version: %s}", category, format, version)));
                .orElseThrow(() -> new IllegalArgumentException(String.format("No match for: {format: %s, version: %s}", format, version)));
    }


    public static boolean isLive(@NonNull IExtractionType format) {
        return !isProduct(format);
    }

    public static boolean isProduct(@NonNull IExtractionType format) {
        return (format.getId() != null && format.getId() >= 0)
            || (format.getLabel() != null && format.getLabel().indexOf('-') != -1);
    }

    public static boolean isAggregation(@NonNull IExtractionType format) {
        return (format.getLabel() != null
            && format.getLabel().toUpperCase().startsWith(AggSpecification.FORMAT_PREFIX))
            || (format.getFormat() != null
            && format.getFormat().toUpperCase().startsWith(AggSpecification.FORMAT_PREFIX));
    }

    public static boolean isPublic(@NonNull IExtractionType format) {
        return isProduct(format) && Objects.equals(format.getStatusId(), StatusEnum.ENABLE.getId());
    }


    public static IExtractionType getByFormat(String format) {
        return getByFormatAndVersion(format, null);
    }

    public static IExtractionType getByFormatAndVersion(String format, String version) {
        Preconditions.checkNotNull(format);
        if (format.toUpperCase().startsWith(AggSpecification.FORMAT_PREFIX)) {
            return AggExtractionTypeEnum.valueOf(format, version);
        }
        return LiveExtractionTypeEnum.valueOf(format, version);
    }

    public static IExtractionType getByExample(@NonNull IExtractionType source) {
        //Preconditions.checkArgument(isLive(source));
        if (isAggregation(source)) {
            return findOneMatch(AggExtractionTypeEnum.values(), source);
        }
        return findOneMatch(LiveExtractionTypeEnum.values(), source);
    }
}
