package net.sumaris.core.model.referential.grouping;

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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;

import javax.persistence.*;

/**
 * GroupingItem permet de lister les entités du référentiel qui appartiennent à un regroupement.
 */
@Data
@FieldNameConstants
@Entity
@Table(name = "grouping_item")
public class GroupingItem implements IEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROUPING_ITEM_SEQ")
    @SequenceGenerator(name = "GROUPING_ITEM_SEQ", sequenceName="GROUPING_ITEM_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "object_id")
    private Integer objectId;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grouping_fk", nullable = false)
    private Grouping grouping;
}
