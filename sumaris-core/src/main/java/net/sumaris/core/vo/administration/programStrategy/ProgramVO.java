package net.sumaris.core.vo.administration.programStrategy;

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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@FieldNameConstants
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ProgramVO implements IReferentialVO {

    @EqualsAndHashCode.Include
    private Integer id;
    private String label;
    private String name;
    private String description;
    private String comments;
    private Date updateDate;
    private Date creationDate;

    private Integer statusId;

    private Map<String, String> properties;

    private Integer taxonGroupTypeId;
    private ReferentialVO taxonGroupType;

    private Integer gearClassificationId;
    private ReferentialVO gearClassification;

    private List<Integer> locationClassificationIds;
    private List<ReferentialVO> locationClassifications;

    private List<Integer> locationIds;
    private List<ReferentialVO> locations;

    private List<StrategyVO> strategies;

    private List<ProgramDepartmentVO> departments;
    private List<ProgramPersonVO> persons;

    private List<String> acquisitionLevelLabels;
    private List<ReferentialVO> acquisitionLevels;

}
