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

import javax.persistence.Id;
import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ReferentialVO implements IReferentialVO<Integer>,
    IReferentialWithLevelVO<Integer> {

    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;
    @ToString.Include
    private String label;
    @ToString.Include
    private String name;
    private String description;
    private String comments;
    private Date updateDate;
    private Date creationDate;

    private Integer statusId;
    private Integer validityStatusId;

    @ToString.Include
    private Integer levelId;
    private ReferentialVO level;

    @ToString.Include
    private Integer parentId;
    private ReferentialVO parent;

    private Integer rankOrder;

    // Metadata
    @ToString.Include
    private String entityName;

    @ToString.Exclude
    private Map<String, Object> properties;
}

