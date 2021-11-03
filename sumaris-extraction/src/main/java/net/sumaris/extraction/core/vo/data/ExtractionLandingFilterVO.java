package net.sumaris.extraction.core.vo.data;

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

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.vo.filter.LandingFilterVO;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExtractionLandingFilterVO extends LandingFilterVO {

    private boolean preview;

    private String sheetName;

    private Page page;

    public String toString(String separator) {
        separator = (separator == null) ? ", " : separator;
        StringBuilder sb = new StringBuilder();
        if (this.getProgramLabel() != null) sb.append(separator).append("Program (label): ").append(this.getProgramLabel());
        if (this.getStartDate() != null) sb.append(separator).append("Start date: ").append(this.getStartDate());
        if (this.getEndDate() != null) sb.append(separator).append("End date: ").append(this.getEndDate());
        if (this.getLocationId() != null) sb.append(separator).append("Location (id): ").append(this.getLocationId());
        if (this.getVesselId() != null) sb.append(separator).append("Vessel (id): ").append(this.getVesselId());
        if (this.getObservedLocationId() != null) sb.append(separator).append("Observed location (id): ").append(this.getObservedLocationId());
        if (this.getRecorderPersonId() != null) sb.append(separator).append("Recorder person (id): ").append(this.getRecorderPersonId());
        if (this.getRecorderDepartmentId() != null) sb.append(separator).append("Recorder department (id): ").append(this.getRecorderDepartmentId());
        return sb.toString();
    }
}
