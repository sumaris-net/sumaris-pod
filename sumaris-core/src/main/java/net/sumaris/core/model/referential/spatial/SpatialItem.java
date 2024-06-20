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
import net.sumaris.core.model.referential.IReferentialEntity;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Présence d'un élément d'une liste du référentiel sur une ou plusieurs zones géographiques.
 *
 * Une ou plusieurs géométries (ponctuelle, linéaire ou polygonale) peuvent être définies afin de géolocaliser un des éléments d'une liste.
 *
 * Une procédure stockée sous Oracle permet la mise à jour d'un lien vers le référentiel des lieux (table SPATIAL_ITEM2LOCATION) à partir des géométries définies.
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "spatial_item")
public class SpatialItem implements IReferentialEntity<Integer> {

    public static final String ENTITY_NAME = "SpatialItem";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SPATIAL_ITEM_SEQ")
    @SequenceGenerator(name = "SPATIAL_ITEM_SEQ", sequenceName="SPATIAL_ITEM_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spatial_item_type_fk", nullable = false)
    private SpatialItemType spatialItemType;

    @Column(name = "object_id", nullable = false)
    private Integer objectId;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = SpatialItem2Location.class, mappedBy = SpatialItem2Location.Fields.SPATIAL_ITEM)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<SpatialItem2Location> locations = new ArrayList<>();

}
