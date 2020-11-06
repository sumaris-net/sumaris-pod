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

package net.sumaris.core.extraction.format;

import com.google.common.base.Preconditions;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.format.specification.AggSpecification;
import net.sumaris.core.extraction.util.ExtractionBeans;
import net.sumaris.core.extraction.vo.AggregationContextVO;
import net.sumaris.core.extraction.vo.ExtractionCategoryEnum;
import net.sumaris.core.extraction.vo.ExtractionContextVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class ExtractionFormats extends ExtractionBeans {

    protected ExtractionFormats()  {

    }

    public static <C extends IExtractionFormat> C checkAndGet(Collection<C> availableTypes, C type) throws IllegalArgumentException {
        Preconditions.checkNotNull(type, "Missing argument 'type' ");
        Preconditions.checkNotNull(type.getLabel(), "Missing argument 'type.label'");

        // Retrieve the extraction type, from list
        final String label = type.getLabel();
        final String version = type.getVersion();
        final ExtractionCategoryEnum category = type.getCategory();
        if (category == null) {
            type = availableTypes.stream()
                    .filter(aType -> label.equalsIgnoreCase(aType.getLabel()) // Same label
                            // Same version
                            && (version == null || version.equalsIgnoreCase(aType.getVersion())))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown extraction type {label: '%s', version: '%s'}", label, version)));
        } else {

            type = availableTypes.stream()
                    .filter(aType -> label.equalsIgnoreCase(aType.getLabel()) // Same label
                            // Same category
                            && category == aType.getCategory()
                            // Same version
                            && (version == null || version.equalsIgnoreCase(aType.getVersion()))
                    )
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown extraction type {category: '%s', label: '%s', version: '%s'}", category, label, version)));
        }
        return type;
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
        return getLiveFormatFromLabel(source.getLabel());
    }

    public static LiveFormatEnum getLiveFormat(ExtractionTypeVO source) {
        return getLiveFormatFromLabel(source.getLabel());
    }

    public static LiveFormatEnum getLiveFormat(AggregationContextVO source) {
        return getLiveFormatFromLabel(source.getLabel());
    }

    public static ProductFormatEnum getAggFormat(ExtractionProductVO source) {
        return getAggFormatFromLabel(source.getLabel());
    }

    public static IExtractionFormat getFormatFromLabel(String label) {
        Preconditions.checkNotNull(label);
        if (label.toUpperCase().startsWith(AggSpecification.FORMAT_PREFIX)) {
            return getAggFormatFromLabel(label);
        }
        return getLiveFormatFromLabel(label);
    }

    public static LiveFormatEnum getLiveFormatFromLabel(String label) {

        final String formatStr = label == null ? null :
                StringUtils.changeCaseToUnderscore(label.split("-")[0]).toUpperCase();

        return LiveFormatEnum.fromString(formatStr)
                .orElseGet(() -> {
                    if (formatStr != null && formatStr.contains(LiveFormatEnum.RDB.name())) {
                        return LiveFormatEnum.RDB;
                    }
                    throw new SumarisTechnicalException(String.format("Extraction format '%s' not implemented !", formatStr));
                });
    }

    public static ProductFormatEnum getAggFormatFromLabel(String label) {

        final String formatStr = label == null ? null :
                StringUtils.changeCaseToUnderscore(label.split("-")[0]).toUpperCase();

        return ProductFormatEnum.fromString(formatStr)
                .orElseGet(() -> {
                    if (formatStr != null && formatStr.contains(ProductFormatEnum.AGG_RDB.name())) {
                        return ProductFormatEnum.AGG_RDB;
                    }
                    throw new SumarisTechnicalException(String.format("Aggregation format '%s' not implemented !", formatStr));
                });
    }
}
