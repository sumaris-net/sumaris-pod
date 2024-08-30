package net.sumaris.extraction.core.vo.report;
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

import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.sumaris.extraction.core.vo.AggregationContextVO;

import java.util.*;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ExtractionMonitoringContextVO extends AggregationContextVO {
    //    ActivityCalendarFilterVO activityFilter;
    // Table names
    String rawTableName;
    String resultTableName;
    // Sheet names
    String sheetName;

    Date startRequestDate;
    Date endRequestDate;

    Integer year;

    Date minDate;
    Date maxDate;

    String strategyTableName; // ST table
    String strategyMonitoringTableName; // SM table

    // Sheet names
    String strategySheetName; // ST
    String strategyMonitoringSheetName; // SM

    @Override
    public Set<String> getTableNames() {
        HashSet<String> result = Sets.newHashSet();
        result.add(resultTableName);
        return result;
    }

    @Override
    public Set<String> getRawTableNames() {
        HashSet<String> result = Sets.newHashSet();
        result.add(resultTableName);
        result.add(rawTableName);
        return result;
    }

    @Override
    public Optional<String> findTableNameBySheetName(@NonNull String sheetName) {
        return Optional.of(resultTableName);
    }
}
