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
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.format.ProductFormatEnum;
import net.sumaris.extraction.core.specification.data.trip.AggSpecification;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class ExtractionFormats  {

    protected ExtractionFormats()  {
        // Helper class
    }

    public static <C extends IExtractionFormat, T extends IExtractionFormat> C findOneMatch(Collection<C> availableTypes,
                                                                                            T format) throws IllegalArgumentException {
        return findOneMatch(availableTypes.stream(), format);
    }

    public static <C extends IExtractionFormat, T extends IExtractionFormat> C findOneMatch(@NonNull Stream<C> availableTypes,
                                                                                            @NonNull T format) throws IllegalArgumentException {
        Preconditions.checkNotNull(format.getLabel(), "Missing argument 'format.label'");

        // Retrieve the extraction type, from list
        final String label = format.getLabel();
        final String version = format.getVersion();
        final ExtractionCategoryEnum category = format.getCategory();
        return availableTypes
                .filter(aType -> label.equalsIgnoreCase(aType.getLabel()) // Same label
                        // Same category
                        && (category == null || category.equals(aType.getCategory()))
                        // Same version
                        && (version == null || version.equals(aType.getVersion()))
                )
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("No match for: {category: %s, label: %s, version: %s}", category, label, version)));
    }

    public static String getTableName(ExtractionContextVO context, String sheetName) {

        String tableName = null;
        if (StringUtils.isNotBlank(sheetName)) {
            tableName = context.getTableNameBySheetName(sheetName);
            Preconditions.checkArgument(tableName != null,
                    String.format("Invalid sheet {%s}: not exists on product {%s}", sheetName, context.getLabel()));
        }

        // Or use first table
        else if (CollectionUtils.isNotEmpty(context.getTableNames())) {
            tableName = context.getTableNames().iterator().next();
        }

        // Table name not found: compute it from context label
        if (tableName == null) {
            if (StringUtils.isBlank(sheetName)) {
                tableName = "P_" + context.getLabel() + "_" + sheetName.toUpperCase();
            }
            else {
                tableName = "P_" + context.getLabel();
            }
        }

        return tableName;
    }

    public static String getTableName(ExtractionProductVO product, String sheetName) {
        Preconditions.checkNotNull(product);
        Preconditions.checkArgument(product.getLabel() != null || sheetName != null);

        String tableName = null;
        if (StringUtils.isNotBlank(sheetName)) {
            tableName = product.findTableNameBySheetName(sheetName)
                    .orElseThrow(() -> new SumarisTechnicalException(String.format("Invalid sheet {%s}: not exists on product {%s}", sheetName, product.getLabel())));
        }

        // Or use first table
        else if (CollectionUtils.isNotEmpty(product.getTables())) {
            tableName = product.getTableNames().get(0);
        }

        if (StringUtils.isNotBlank(tableName)) return tableName;

        // Table name not found: compute it (from the context label)
        return "p_" + product.getLabel().toLowerCase();
    }

    public static LiveFormatEnum getLiveFormat(ExtractionProductVO source) {
        return LiveFormatEnum.valueOf(source.getLabel(), source.getVersion());
    }

    public static LiveFormatEnum getLiveFormat(IExtractionFormat format) {
        return LiveFormatEnum.valueOf(format.getLabel(), format.getVersion());
    }

    public static ProductFormatEnum getProductFormat(IExtractionFormat format) {
        return ProductFormatEnum.valueOf(format.getLabel(), format.getVersion());
    }

    public static IExtractionFormat getFormatFromLabel(String label) {
        return getFormatFromLabel(label, null);
    }

    public static IExtractionFormat getFormatFromLabel(String label, String version) {
        Preconditions.checkNotNull(label);
        if (label.toUpperCase().startsWith(AggSpecification.FORMAT_PREFIX)) {
            return ProductFormatEnum.valueOf(label, version);
        }
        return LiveFormatEnum.valueOf(label, version);
    }
}
