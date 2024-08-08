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

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.vo.filter.ActimonitFilterVO;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ExtractionActimonitContextVO extends ExtractionContextVO {

    // Table names
    String processTableName;

    // Sheet names
    String processSheetName;

    ActimonitFilterVO actimonitFilter;

    public Date getStartDate() {
        return actimonitFilter != null ? actimonitFilter.getStartDate() : null;
    }

    public Date getEndDate() {
        return actimonitFilter != null ? actimonitFilter.getEndDate() : null;
    }


    public List<String> getProgramLabels() {
        return actimonitFilter != null && StringUtils.isNotBlank(actimonitFilter.getProgramLabel()) ? ImmutableList.of(actimonitFilter.getProgramLabel()) : null;
    }

    public List<Integer> getVesselIds() {
        return actimonitFilter != null && actimonitFilter.getActimonitId() != null ? ImmutableList.of(actimonitFilter.getActimonitId()) : null;
    }

    public List<Integer> getBasePortLocationIds() {
        return actimonitFilter != null && actimonitFilter.getBasePortLocationId() != null ? ImmutableList.of(actimonitFilter.getBasePortLocationId()) : null;
    }

    public List<Integer> getRegistrationLocationIds() {
        return actimonitFilter != null && actimonitFilter.getRegistrationLocationId() != null ? ImmutableList.of(actimonitFilter.getRegistrationLocationId()) : null;
    }

    public List<Integer> getStatusIds() {
        return actimonitFilter != null ? actimonitFilter.getStatusIds() : null;
    }
}
