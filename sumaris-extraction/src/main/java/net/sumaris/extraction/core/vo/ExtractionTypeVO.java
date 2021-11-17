package net.sumaris.extraction.core.vo;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
public class ExtractionTypeVO implements IValueObject<Integer>,
        IExtractionFormat,
        IWithRecorderDepartmentEntity<Integer, DepartmentVO>,
        IWithRecorderPersonEntity<Integer, PersonVO> {

    Integer id;

    @ToString.Include
    ExtractionCategoryEnum category;
    @ToString.Include
    String label;
    @ToString.Include
    String name;
    @ToString.Include
    String version;

    String description;
    String comments;
    String[] sheetNames;
    Integer statusId;
    Boolean isSpatial;
    String docUrl;

    /**
     * The extraction filter used to create data. Useful to refresh the aggregation
     */
    String filter;
    Integer processingFrequencyId;

    @JsonIgnore
    LiveFormatEnum liveFormat;

    PersonVO recorderPerson;
    DepartmentVO recorderDepartment;

    @JsonIgnore
    public boolean isPublic() {
        return statusId != null && statusId == StatusEnum.ENABLE.getId();
    }
}
