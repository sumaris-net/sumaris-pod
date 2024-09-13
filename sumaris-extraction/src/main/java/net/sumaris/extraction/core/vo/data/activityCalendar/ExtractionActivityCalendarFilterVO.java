package net.sumaris.extraction.core.vo.data.activityCalendar;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2024 SUMARiS Consortium
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

import com.google.common.base.Joiner;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import org.apache.activemq.store.kahadb.disk.page.Page;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class ExtractionActivityCalendarFilterVO extends ActivityCalendarFilterVO {
    private boolean preview;

    private String sheetName;

    private Page page;

    List<String> registrationLocationLabels;

    List<String> basePortLocationLabels;

    List<String> vesselRegistrationCodes;

    public String toString(String separator) {
        separator = (separator == null) ? ", " : separator;
        StringBuilder sb = new StringBuilder();
        if (this.getProgramLabel() != null) sb.append(separator).append("Program (label): ").append(this.getProgramLabel());
        if (this.getYear() != null) sb.append(separator).append("Year: ").append(this.getYear());
        if (this.getStartDate() != null) sb.append(separator).append("Start date: ").append(this.getStartDate());
        if (this.getEndDate() != null) sb.append(separator).append("End date: ").append(this.getEndDate());
        if (! this.registrationLocationLabels.isEmpty()) sb.append(separator).append("Registration location (labels): ").append(Joiner.on(",").join(this.getRegistrationLocationLabels()));
        if (! this.basePortLocationLabels.isEmpty()) sb.append(separator).append("Base port location (labels): ").append(Joiner.on(",").join(this.getBasePortLocationLabels()));
        if (! this.vesselRegistrationCodes.isEmpty()) sb.append(separator).append("Base port location (labels): ").append(Joiner.on(",").join(this.getBasePortLocationLabels()));
        return sb.toString();
    }
}
