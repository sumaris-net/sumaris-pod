package net.sumaris.extraction.core.vo;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.IAggregationSourceVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;

import java.util.*;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationContextVO extends ExtractionContextVO implements IAggregationSourceVO {

    AggregationStrataVO strata;

    boolean enableAnalyze = true;

    @FieldNameConstants.Exclude
    Map<String, Map<String, List<String>>> columnValues = new LinkedHashMap<>();

    @FieldNameConstants.Exclude
    Map<String, Set<String>> spatialColumnNames = new LinkedHashMap<>();

    public void addTableName(String tableName, String sheetName,
                             Set<String> hiddenColumnNames,
                             Set<String> spatialColumnNames,
                             Map<String,List<String>> availableValuesByColumn) {
        super.addTableName(tableName, sheetName, hiddenColumnNames, false);

        if (CollectionUtils.isNotEmpty(spatialColumnNames)) {
            this.spatialColumnNames.put(tableName, spatialColumnNames);
        }
        if (MapUtils.isNotEmpty(availableValuesByColumn)) {
            columnValues.put(tableName, availableValuesByColumn);
        }
    }


    /**
     * Return the hidden columns of the given table
     * @param tableName
     */
    public boolean hasSpatialColumn(String tableName) {
        return CollectionUtils.isNotEmpty(spatialColumnNames.get(tableName));
    }

    public boolean isSpatial() {
        return Beans.getStream(getTableNames())
                .anyMatch(this::hasSpatialColumn);
    }

    public Map<String, List<String>> getColumnValues(String tableName) {
        return columnValues.get(tableName);
    }

    public String getStrataSpaceColumnName() {
        return strata != null ? strata.getSpatialColumnName() : null;
    }

    public String getStrataTimeColumnName() {
        return strata != null ? strata.getTimeColumnName() : null;
    }

    @Override
    public List<AggregationStrataVO> getStratum() {
        return ImmutableList.of(strata);
    }
}
