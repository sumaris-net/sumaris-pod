package net.sumaris.extraction.core.vo.trip.rdb;

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

import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import net.sumaris.extraction.core.vo.trip.AggregationTripContextVO;

import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationRdbTripContextVO extends AggregationTripContextVO {
    String stationTableName; // HH table
    String speciesListTableName; // SL table
    String speciesLengthTableName; // HL table
    String sampleTableName; // CA table

    String speciesLengthMapTableName; // Map to get SL.ID from HL.ID

    String landingTableName; // CL table

    // Allow to rename column name (e.g FREE1 use FISHING_DURATION instead of FISHING_TIME)
    Map<String, String> columnNamesMapping = Maps.newLinkedHashMap();

    public <C extends AggregationRdbTripContextVO> C addColumnNameReplacement(String sourceColumnName, String targetColumnName) {
        columnNamesMapping.put(sourceColumnName, targetColumnName);
        return (C)this;
    }
}
