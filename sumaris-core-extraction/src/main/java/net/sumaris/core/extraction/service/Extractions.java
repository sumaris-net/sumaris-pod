package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.vo.ExtractionContextVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class Extractions {

    protected Extractions()  {

    }

    public static <C extends ExtractionTypeVO> C checkAndFindType(Collection<C> availableTypes, C type) throws IllegalArgumentException {
        Preconditions.checkNotNull(type, "Missing argument 'type' ");
        Preconditions.checkNotNull(type.getLabel(), "Missing argument 'type.label'");

        // Retrieve the extraction type, from list
        final String extractionLabel = type.getLabel();
        if (type.getCategory() == null) {
            type = availableTypes.stream()
                    .filter(aType -> aType.getLabel().equalsIgnoreCase(extractionLabel))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown extraction type label {%s}", extractionLabel)));
        } else {
            final String extractionCategory = type.getCategory();
            type = availableTypes.stream()
                    .filter(aType -> aType.getLabel().equalsIgnoreCase(extractionLabel) && aType.getCategory().equalsIgnoreCase(extractionCategory))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown extraction type category/label {%s/%s}", extractionCategory, extractionLabel)));
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
            tableName = product.getTableNameBySheetName(sheetName)
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
}
