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

package net.sumaris.core.vo.data.batch;

import com.google.common.base.Joiner;
import net.sumaris.core.dao.data.batch.BatchSpecifications;
import net.sumaris.core.util.UnicodeChars;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper class
 */
public class DenormalizedBatches {

    protected DenormalizedBatches() {
        // Helper class
    }

    public static boolean isExhaustiveInventory(DenormalizedBatchVO b) {
        return !DenormalizedBatches.isCatchBatch(b)
            && (b.getInheritedTaxonName() != null || Boolean.TRUE.equals(b.getExhaustiveInventory()));
    }

    public static boolean isCatchBatch(DenormalizedBatchVO b) {
        return !b.hasParent() || BatchSpecifications.DEFAULT_ROOT_BATCH_LABEL.equals(b.getLabel());
    }

    public static boolean isSamplingBatch(DenormalizedBatchVO b) {
        return b.getSamplingRatio() != null ||
            (
                // Should have a parent, and parent should have only one child
                b.getParent() != null && CollectionUtils.size(b.getParent().getChildren()) == 1
                // Self or parent should have a weight
                && (b.getParent().getWeight() != null && b.getWeight() != null)
                // Should not have sorting values, nor taxon or reference taxon
                && !hasOwnedSortingValue(b) && b.getTaxonGroup() == null && b.getTaxonName() == null
            );
    }

    public static boolean hasOwnedSortingValue(DenormalizedBatchVO b) {
        return hasSortingValue(b, false);
    }

    public static boolean hasSortingValue(DenormalizedBatchVO b, boolean includeInheritedValues) {
        if (CollectionUtils.isEmpty(b.getSortingValues())) return false;
        return b.getSortingValues().stream()
                .anyMatch(sv -> includeInheritedValues ? true : !sv.getIsInherited());
    }

    public static boolean isParentOfSamplingBatch(DenormalizedBatchVO b) {
        return CollectionUtils.size(b.getChildren()) == 1
                && isSamplingBatch(b.getChildren().get(0));
    }

    public static double computeFlatOrder(DenormalizedBatchVO b) {
        return (b.getParent() != null ? computeFlatOrder(b.getParent()) : 0d)
                + (b.getRankOrder() != null ? b.getRankOrder().doubleValue() : 1d) * Math.pow(10, -1 * (b.getTreeLevel() - 1 ) * 3);
    }

    public static String dumpAsString(List<? extends DenormalizedBatchVO> sources,
                                      boolean withHierarchicalLabel,
                                      boolean useUnicode) {

        Joiner joiner = Joiner.on(' ').skipNulls();

        return sources.stream()
                .sorted(Comparator.comparing(DenormalizedBatches::computeFlatOrder))
                .map(source -> {
                    String treeIndent = useUnicode ? replaceTreeUnicode(source.getTreeIndent()) : source.getTreeIndent();
                    String hierarchicalLabel = withHierarchicalLabel ? generateHierarchicalLabel(source) : null;
                    String elevateFactor = getElevateFactor(source).toString();
                    boolean hasSpecies = source.getTaxonGroup() != null || source.getTaxonName() != null;
                    return joiner.join(
                            treeIndent,
                            hierarchicalLabel != null ? (hierarchicalLabel + " -") : null,

                        // Is exhaustive inventory ?
                        getExhaustiveInventoryAsString(source, true),

                        // Fish icon
                        useUnicode && hasSpecies ? UnicodeChars.FISH : null,

                        // Taxon group
                        getLabelOrNull(source.getTaxonGroup(), false),

                        // Taxon name
                        getLabelOrNull(source.getTaxonName(), false),

                        // Sorting values, or '%' if fraction
                        source.getParent() == null ? source.getLabel() :
                                (isSamplingBatch(source) ? "fraction" : source.getSortingValuesText()),

                        // Sampling ratio
                        StringUtils.trimToNull(source.getSamplingRatioText()),

                        // Weight
                        (source.getWeight() != null ? String.format("[%s kg]", source.getWeight()) : null),

                        // Indirect weight
                        ((source.getIndirectWeight() != null && !Objects.equals(source.getIndirectWeight(), source.getWeight()))
                                ? String.format("(%s %s kg)", useUnicode ? UnicodeChars.ARROW_DOWN : "~", source.getIndirectWeight()) : null),

                        // Individual count
                        (source.getIndividualCount() != null ? String.format("[%s indiv]", source.getIndividualCount()) : null),

                        // Indirect individual count
                        ((source.getIndirectIndividualCount() != null && !Objects.equals(source.getIndirectIndividualCount(), source.getIndividualCount()))
                                ? String.format("(%s %s indiv)", useUnicode ? UnicodeChars.ARROW_DOWN : "~", source.getIndirectIndividualCount()) : null),

                        // Elevate factor
                        (!"1".equals(elevateFactor) ? String.format("x%s", elevateFactor) : null),

                        "=>",

                        // Elevated weight
                        (source.getElevateWeight() != null ? String.format("%s kg", source.getElevateWeight()) : null),

                        // Elevated individual count
                        (source.getElevateIndividualCount() != null ? String.format("%s indiv", source.getElevateIndividualCount()) : null)

                        // Elevation factor
                        //, String.format("(x %s)", ((TempDenormalizedBatchVO)source).getElevateFactor())
                    ).replace("[ ]+", " ");
                }).collect(Collectors.joining("\n"));
    }

    /* -- internal functions -- */

    protected static BigDecimal getElevateFactor(DenormalizedBatchVO source) {
        if (source instanceof TempDenormalizedBatchVO) {
            return ((TempDenormalizedBatchVO)source).getElevateFactor();
        }
        return new BigDecimal(1);
    }
    protected static String replaceTreeUnicode(String treeIndent) {
        return treeIndent.replace("|-", "\u02EB")
                .replace("|_", "\u02EA")
                .replace(" ", "\t")
                .replace("\t\t", "\t");
    }

    protected static String getExhaustiveInventoryAsString(DenormalizedBatchVO source, boolean useUnicode) {
        if (source.getTaxonGroup() == null) return null;
        boolean exhaustiveInventory = source.getExhaustiveInventory() != null ? source.getExhaustiveInventory() : false;
        if (useUnicode) {
            return exhaustiveInventory ? "\u2705" : "\u274E";
        }
        return exhaustiveInventory ? "[x]" : "[ ]";
    }

    protected static String generateHierarchicalLabel(DenormalizedBatchVO source) {
        if (source.getParent() == null) return null;
        return Joiner.on('.').skipNulls().join(generateHierarchicalLabel(source.getParent()), source.getRankOrder());
    }

    protected static String getLabelOrNull(@Nullable IReferentialVO referential, boolean withAccolade) {
        if (referential == null) return null;
        if (!withAccolade) return referential.getLabel();
        return "{" + referential.getLabel() + "}";
    }
}
