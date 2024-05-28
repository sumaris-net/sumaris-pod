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
import lombok.NonNull;
import net.sumaris.core.dao.data.batch.BatchSpecifications;
import net.sumaris.core.model.referential.QualityFlags;
import net.sumaris.core.model.referential.pmfm.ParameterEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Numbers;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.UnicodeChars;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
            // Batch a taxon => always exhaustive (not child with another taxon)
            && (b.getInheritedTaxonName() != null
            || (
                // If batch is marked has exhaustive
                Boolean.TRUE.equals(b.getExhaustiveInventory())
            )
       );
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
                .anyMatch(sv -> includeInheritedValues || !sv.getIsInherited());
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

        String arrowDown = useUnicode ? UnicodeChars.ARROW_DOWN : "~";

        return sources.stream()
                .sorted(Comparator.comparing(DenormalizedBatches::computeFlatOrder))
                // Cast as temp batch (more fields can be displayed)
                .map(vo -> (vo instanceof TempDenormalizedBatchVO)
                    ? (TempDenormalizedBatchVO) vo
                    : Beans.clone(vo, TempDenormalizedBatchVO.class))
                .map(source -> {
                    String treeIndent = useUnicode ? replaceTreeUnicode(source.getTreeIndent()) : source.getTreeIndent();
                    String hierarchicalLabel = withHierarchicalLabel ? generateHierarchicalLabel(source) : null;
                    double elevateContextFactor = Numbers.doubleValue(source.getElevateContextFactor(), 1d);
                    double aliveFactor = source.getAliveWeightFactor() != null ? source.getAliveWeightFactor() : 1d;
                    boolean hasSpecies = source.getTaxonGroup() != null || source.getTaxonName() != null;
                    return joiner.join(
                        // Tree indentation
                        treeIndent,

                        // Computed label
                        hierarchicalLabel != null ? (hierarchicalLabel + " -") : null,

                        // DEBUG -- batch id
                        //source.getId(),

                        // Is exhaustive inventory ?
                        getExhaustiveInventoryAsString(source, useUnicode),

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
                                ? String.format("(%s%s kg)", arrowDown, source.getIndirectWeight()) : null),

                        // RTP Context Weight
                        (source.getRtpContextWeight() != null ? String.format("(RTP=%s kg)", source.getRtpContextWeight()) : null),

                        // Indirect RTP Context Weight
                        ((source.getIndirectRtpContextWeight() != null && !Objects.equals(source.getIndirectRtpContextWeight(), source.getRtpContextWeight()))? String.format("(%sRTP=%skg)", arrowDown, source.getIndirectRtpWeight()) : null),

                        // Individual count
                        (source.getIndividualCount() != null ? String.format("[%s indiv]", source.getIndividualCount()) : null),

                        // Indirect individual count
                        ((source.getIndirectIndividualCount() != null && !Objects.equals(source.getIndirectIndividualCount(), source.getIndividualCount()))
                                ? String.format("(%s%s indiv)", arrowDown, source.getIndirectIndividualCount()) : null),

                        // Elevate context factor
                        (1d != elevateContextFactor ? String.format("x%s", elevateContextFactor) : null),

                        // Alive factor = elevate factor / context factor)
                        (1d != aliveFactor ? String.format("x%s", aliveFactor) : null),

                        "=>",

                        // Elevated weight
                        ((source.getElevateWeight() != null && !Objects.equals(source.getElevateWeight(), source.getIndirectElevateWeight())) ? String.format("%s kg", source.getElevateWeight()) : null),

                        // Indirect elevated weight
                        (source.getIndirectElevateWeight() != null ? String.format("(%s%s kg)", arrowDown, source.getIndirectElevateWeight()) : null),

                        // Elevated RTP weight
                        ((source.getElevateRtpWeight() != null && !Objects.equals(source.getElevateRtpWeight(), source.getIndirectRtpElevateWeight()))? String.format("(RTP=%s kg)", source.getElevateRtpWeight()) : null),

                        // Indirect elevated RTP weight
                        (source.getIndirectRtpElevateWeight() != null ? String.format("(%sRTP=%s kg)", arrowDown, source.getIndirectRtpElevateWeight()) : null),

                        // Elevated individual count
                        (source.getElevateIndividualCount() != null ? String.format("%s indiv", source.getElevateIndividualCount()) : null)

                    ).replace("[ ]+", " ");
                }).collect(Collectors.joining("\n"));
    }

    public static boolean hasSomeInvalidChild(DenormalizedBatchVO source) {
        if (!source.hasChildren()) return false;
        return source.getChildren().stream()
            .anyMatch(child -> QualityFlags.isInvalid(child.getQualityFlagId())
                || hasSomeInvalidChild(child) // Loop on children
            );
    }

    public static Optional<Integer> getSexId(DenormalizedBatchVO batch) {
        return getSortingQualitativeValueIdByParameterId(batch, ParameterEnum.SEX.getId());
    }

    public static Optional<Integer> getDressingId(DenormalizedBatchVO batch) {
        return getSortingQualitativeValueIdByPmfmId(batch, PmfmEnum.DRESSING.getId());
    }

    public static Optional<Integer> getPreservationId(DenormalizedBatchVO batch) {
        return getSortingQualitativeValueIdByPmfmId(batch, PmfmEnum.PRESERVATION.getId());
    }

    public static Optional<Integer> getSortingQualitativeValueIdByPmfmId(DenormalizedBatchVO batch, int pmfmId) {
        return getSortingQualitativeValueIdByFilter(
            batch,
            sv -> sv.getPmfmId() == pmfmId || (sv.getPmfm() != null && sv.getPmfm().getId() == pmfmId)
        );
    }

    public static Optional<Integer> getSortingQualitativeValueIdByParameterId(DenormalizedBatchVO batch, int parameterId) {
        return getSortingQualitativeValueIdByFilter(
            batch,
            sv -> sv.getParameter() != null && sv.getParameter().getId() == parameterId
        );
    }

    public static Optional<Integer> getSortingQualitativeValueIdByFilter(DenormalizedBatchVO batch, Predicate<DenormalizedBatchSortingValueVO> filterFn) {
        return Beans.getStream(batch.getSortingValues())
            .filter(sv -> sv.getQualitativeValue() != null)
            .filter(filterFn::test)
            .map(DenormalizedBatchSortingValueVO::getQualitativeValue)
            .map(ReferentialVO::getId)
            .findFirst();
    }

    /**
     * Compute diff (%) between two weights
     * @param weight1
     * @param weight2
     * @return
     */
    public static double computeWeightDiffPercent(@NonNull Number weight1, @NonNull Number weight2) {
        BigDecimal w1 = new BigDecimal(weight1.toString());
        BigDecimal w2 = new BigDecimal(weight2.toString());

        // (ABS(w1 - w2) / w1) * 100
        return w1.subtract(w2).abs()
            .divide(w1)
            .multiply(new BigDecimal(100))
            // Round to 2 decimal
            .divide(new BigDecimal(1), 2, RoundingMode.HALF_UP)
            .doubleValue();
    }


    /* -- internal functions -- */

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
