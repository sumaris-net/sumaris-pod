package net.sumaris.importation.core.service.activitycalendar.vo;

/*-
 * #%L
 * SUMARiS:: Importation
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.File;

@Data
@Builder
public class ActivityCalendarImportContextVO {
    @NonNull
    private Integer recorderPersonId;

    @NonNull
    private File processingFile;

    // result object containing messages and errors during process
    @NonNull
    @Builder.Default
    @JsonIgnore
    private ActivityCalendarImportResultVO result = new ActivityCalendarImportResultVO();
}
