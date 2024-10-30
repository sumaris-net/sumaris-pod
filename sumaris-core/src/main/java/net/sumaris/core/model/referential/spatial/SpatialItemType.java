package net.sumaris.core.model.referential.spatial;

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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.ObjectType;
import net.sumaris.core.model.referential.Status;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Type de régionalisation d'une liste du référentiel.
 *
 * <p>Il peut y avoir plusieurs types de régionalisation pour une meme liste (pour un meme OBJECT_TYPE_FK).</p>
 *
 * <p>Par exemple : <ul>
 *     <li>Régionalisation des gradients de profondeur (utile pour les Antilles)</li>
 * </ul>
 * </p>
 *
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "spatial_item_type")
public class SpatialItemType implements IItemReferentialEntity<Integer> {

    public static final String ENTITY_NAME = "SpatialItemType";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SPATIAL_ITEM_TYPE_SEQ")
    @SequenceGenerator(name = "SPATIAL_ITEM_TYPE_SEQ", sequenceName="SPATIAL_ITEM_TYPE_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(length = LENGTH_LABEL)
    private String label;

    @Column(nullable = false, length = LENGTH_NAME)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ObjectType.class)
    @JoinColumn(name = "object_type_fk", nullable = false)
    private ObjectType objectType;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = SpatialItem.class, mappedBy = SpatialItem.Fields.SPATIAL_ITEM_TYPE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<SpatialItem> items = new ArrayList<>();

}
