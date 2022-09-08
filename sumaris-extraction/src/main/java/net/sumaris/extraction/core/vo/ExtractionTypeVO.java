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
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
public class ExtractionTypeVO implements IValueObject<Integer>,
    IExtractionType<PersonVO, DepartmentVO>,
    IWithRecorderPersonEntity<Integer, PersonVO>,
    IWithRecorderDepartmentEntity<Integer, DepartmentVO> {

    Integer id;

    @ToString.Include
    String format;
    @ToString.Include
    String version;
    String[] sheetNames;

    @ToString.Include
    String label;
    @ToString.Include
    String name;

    String description;
    String comments;
    Integer statusId;
    Date creationDate;
    Date updateDate;

    Boolean isSpatial;
    String docUrl;
    Integer processingFrequencyId;

    Integer parentId;
    @JsonIgnore
    IExtractionType<PersonVO, DepartmentVO> parent;

    PersonVO recorderPerson;
    DepartmentVO recorderDepartment;

    public ExtractionTypeVO(IExtractionType source) {

        Beans.copyProperties(source, this);

        this.setFormat(source.getFormat());
        this.setVersion(source.getVersion());

        this.setParentId(source.getParentId());
        if (source.getParent() != null) {
            this.setParent(new ExtractionTypeVO(source.getParent()));
        }

        this.setLabel(source.getLabel());
        this.setName(source.getName());
        this.setSheetNames(source.getSheetNames());
        this.setStatusId(source.getStatusId());
        this.setIsSpatial(source.getIsSpatial());

        if (source instanceof IWithRecorderDepartmentEntity) {
            Object recorderDepartment = ((IWithRecorderDepartmentEntity)source).getRecorderDepartment();
            if (recorderDepartment instanceof DepartmentVO) {
                this.setRecorderDepartment((DepartmentVO) recorderDepartment);
            }
        }
        if (source instanceof IWithRecorderPersonEntity) {
            Object recorderPerson = ((IWithRecorderPersonEntity)source).getRecorderPerson();
            if (recorderPerson instanceof PersonVO) {
                this.setRecorderPerson((PersonVO) recorderPerson);
            }
        }
    }


}
