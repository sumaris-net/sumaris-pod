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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldNameConstants
@ToString(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionProductVO implements IReferentialVO<Integer>,
    IWithRecorderDepartmentEntity<Integer, DepartmentVO>,
    IWithRecorderPersonEntity<Integer, PersonVO>,
    IExtractionTypeWithTablesVO,
    IExtractionTypeWithStratumVO,
    IAggregationSourceVO {

    @ToString.Include
    Integer id;

    @ToString.Include
    @Builder.Default
    ExtractionCategoryEnum category = ExtractionCategoryEnum.PRODUCT;
    @ToString.Include
    String format;
    @ToString.Include
    String version;

    @ToString.Include
    String label;
    String name;
    String description;
    String documentation;
    String comments;
    Date updateDate;
    Date creationDate;
    Boolean isSpatial;
    String docUrl;
    String filterContent;
    Integer processingFrequencyId;

    DepartmentVO recorderDepartment;
    PersonVO recorderPerson;

    Integer statusId;
    Integer parentId;

    //@JsonIgnore
    IExtractionType<PersonVO, DepartmentVO> parent;

    List<ExtractionTableVO> tables;
    List<AggregationStrataVO> stratum;

    public ExtractionProductVO(IExtractionType other) {
        Beans.copyProperties(other, this);
        if (other instanceof ExtractionProductVO) {
            tables = ImmutableList.copyOf(Beans.getList(((ExtractionProductVO) other).tables));
            stratum = ImmutableList.copyOf(Beans.getList(((IExtractionTypeWithStratumVO) other).getStratum()));
        }
        else {
            if (other instanceof  IExtractionTypeWithTablesVO) {
                tables = ((IExtractionTypeWithTablesVO) other).getTableNames().stream()
                    .map(tableName -> {
                        String sheetName = ((IExtractionTypeWithTablesVO) other).findSheetNameByTableName(tableName).orElse(null);
                        return ExtractionTableVO.builder()
                            .tableName(tableName)
                            .label(sheetName)
                            .build();
                    })
                    .collect(Collectors.toList());
            }
            if (other instanceof  IExtractionTypeWithStratumVO) {
                stratum = ImmutableList.copyOf(Beans.getList(((IExtractionTypeWithStratumVO) other).getStratum()));
            }
        }
    }

    @Override
    public Set<String> getTableNames() {
        if (tables == null) return null;
        return tables.stream().map(ExtractionTableVO::getTableName).collect(Collectors.toSet());
    }

    @Override
    public Optional<String> findTableNameBySheetName(@NonNull String sheetName) {
        return ListUtils.emptyIfNull(tables).stream()
            .filter(t -> sheetName.equalsIgnoreCase(t.getLabel()))
            .map(ExtractionTableVO::getTableName)
            .findFirst();
    }

    @Override
    public Optional<String> findSheetNameByTableName(@NonNull String tableName) {
        return ListUtils.emptyIfNull(tables).stream()
            .filter(t -> tableName.equalsIgnoreCase(t.getTableName()))
            .map(ExtractionTableVO::getLabel)
            .findFirst();
    }

    public String[] getSheetNames() {
        if (tables == null) return null;
        return tables.stream().map(ExtractionTableVO::getLabel).toArray(String[]::new);
    }

    public Map<String, String> getTableNameBySheetNameMap() {
        if (tables == null) return null;
        return tables.stream()
            .collect(Collectors.toMap(ExtractionTableVO::getLabel, ExtractionTableVO::getTableName));
    }

    public Map<String, Set<String>> getHiddenColumnNames() {
        if (tables == null) return null;
        Map<String, Set<String>> result = Maps.newHashMap();
        Beans.getStream(tables)
            .forEach(table -> {
                Set<String> columnNames = Beans.getStream(table.getColumns())
                    .filter(c -> "hidden".equalsIgnoreCase(c.getType()))
                    .map(ExtractionTableColumnVO::getColumnName)
                    .collect(Collectors.toSet());
                result.put(table.getTableName(), columnNames);
            });
        return result;
    }

}
