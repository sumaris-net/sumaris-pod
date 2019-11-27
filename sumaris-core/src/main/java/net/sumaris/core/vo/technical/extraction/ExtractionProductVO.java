package net.sumaris.core.vo.technical.extraction;

/*-
 * #%L
 * SUMARiS:: Core
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

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.apache.commons.collections4.ListUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldNameConstants
public class ExtractionProductVO implements IReferentialVO,
        IWithRecorderDepartmentEntity<Integer, DepartmentVO>,
        IWithRecorderPersonEntity<Integer, PersonVO> {

    private Integer id;
    private String label;
    private String name;
    private String description;
    private String comments;
    private Date updateDate;
    private Date creationDate;
    private Boolean isSpatial;

    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private Integer statusId;
    private Integer parentId;

    private List<ExtractionProductTableVO> tables;
    private List<ExtractionProductStrataVO> stratum;

    public List<String> getTableNames() {
        if (tables == null) return null;
        return tables.stream().map(t -> t.getTableName()).collect(Collectors.toList());
    }

    public List<String> getSheetNames() {
        if (tables == null) return null;
        return tables.stream().map(t -> t.getLabel()).collect(Collectors.toList());
    }

    public Map<String, String> getItems() {
        if (tables == null) return null;
        return tables.stream().collect(Collectors.toMap(t -> t.getLabel(), t -> t.getTableName()));
    }

    public Optional<String> getTableNameBySheetName(String sheetName) {
        Preconditions.checkNotNull(sheetName);
        return ListUtils.emptyIfNull(tables).stream()
                .filter(t -> sheetName.equalsIgnoreCase(t.getLabel()))
                .map(t -> t.getTableName())
                .findFirst();
    }

    public Optional<String> getSheetNameByTableName(String tableName) {
        Preconditions.checkNotNull(tableName);
        return ListUtils.emptyIfNull(tables).stream()
                .filter(t -> tableName.equalsIgnoreCase(t.getTableName()))
                .map(t -> t.getLabel())
                .findFirst();
    }

    public boolean hasSpatialSheet() {
        return ListUtils.emptyIfNull(tables).stream()
                .anyMatch(t -> t.getIsSpatial() != null && t.getIsSpatial().booleanValue());

    }

}
