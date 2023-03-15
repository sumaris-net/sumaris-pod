package net.sumaris.core.vo.referential;

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

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.ITreeNodeEntity;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReferentialVO implements IReferentialVO<Integer>,
    IReferentialWithLevelVO<Integer> {

    @EqualsAndHashCode.Include
    private Integer id;
    private String label;
    private String name;
    private String description;
    private String comments;
    private Date updateDate;
    private Date creationDate;

    private Integer statusId;
    private Integer validityStatusId;

    //@EqualsAndHashCode.Exclude
    private Integer levelId;
    private ReferentialVO level;
    private Integer parentId;
    private ReferentialVO parent;

    private Integer rankOrder;

    // Metadata
    //@EqualsAndHashCode.Exclude
    private String entityName;

    private Map<String, Object> properties;
}

