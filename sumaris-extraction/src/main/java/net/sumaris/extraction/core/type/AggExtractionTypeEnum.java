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

package net.sumaris.extraction.core.type;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.extraction.core.specification.data.trip.*;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

/**
 * Aggregation format
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Slf4j
public enum AggExtractionTypeEnum implements IExtractionType<PersonVO, DepartmentVO> {

    AGG_RDB (AggRdbSpecification.FORMAT, AggRdbSpecification.SHEET_NAMES, AggRdbSpecification.VERSION_1_3, LiveExtractionTypeEnum.RDB),
    AGG_COST (AggCostSpecification.FORMAT, AggCostSpecification.SHEET_NAMES, AggCostSpecification.VERSION_1_4, LiveExtractionTypeEnum.COST),
    AGG_FREE (AggFree1Specification.FORMAT, AggFree1Specification.SHEET_NAMES, AggFree1Specification.VERSION_1, LiveExtractionTypeEnum.FREE1),
    AGG_SURVIVAL_TEST (AggSurvivalTestSpecification.FORMAT, AggSurvivalTestSpecification.SHEET_NAMES, AggSurvivalTestSpecification.VERSION_1_0, LiveExtractionTypeEnum.SURVIVAL_TEST),
    AGG_PMFM_TRIP(AggPmfmTripSpecification.FORMAT, AggPmfmTripSpecification.SHEET_NAMES, AggPmfmTripSpecification.VERSION_1_0, LiveExtractionTypeEnum.PMFM_TRIP),
    AGG_RJB_TRIP (AggRjbTripSpecification.FORMAT, AggRjbTripSpecification.SHEET_NAMES, AggRjbTripSpecification.VERSION_1_0, LiveExtractionTypeEnum.RJB_TRIP)
    ;

    private Integer id;
    private String format;
    private String[] sheetNames;
    private String version;

    private IExtractionType parent;

    AggExtractionTypeEnum(String format, String[] sheetNames, String version, IExtractionType parent) {
        this.format = format;
        this.sheetNames = sheetNames;
        this.version = version;
        this.parent = parent;
        // A negative integer, to be different from a product identifier
        this.id =  -1 * Math.abs(new HashCodeBuilder()
            .append(format)
            .append(sheetNames)
            .append(version)
            .append(parent)
            .build());
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return getFormat();
    }

    public String getFormat() {
        return format;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public Boolean getIsSpatial() {
        return true;
    }

    public IExtractionType getParent() {
        return parent;
    }

    public String[] getSheetNames() {
        return sheetNames;
    }

    public static AggExtractionTypeEnum valueOf(@NonNull String format, @Nullable String version) {
        return findFirst(format, version)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Unknown product format found, for label '%s'", format)));
    }

    public static Optional<AggExtractionTypeEnum> findFirst(@NonNull IExtractionType format) {
        return findFirst(format.getFormat(), format.getVersion());
    }

    public static Optional<AggExtractionTypeEnum> findFirst(@NonNull String format, String version) {
        return Arrays.stream(values())
                .filter(e -> format.equalsIgnoreCase(e.getFormat())
                        && (version == null || e.getVersion().equalsIgnoreCase(version)))
                .findFirst();
    }

}
