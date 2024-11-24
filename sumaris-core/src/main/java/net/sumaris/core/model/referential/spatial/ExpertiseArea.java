/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.model.referential.spatial;

import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevel;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "expertise_area")
public class ExpertiseArea implements IItemReferentialEntity<Integer>  {

    public static final String ENTITY_NAME = "ExpertiseArea";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EXPERTISE_AREA_SEQ")
    @SequenceGenerator(name = "EXPERTISE_AREA_SEQ", sequenceName="EXPERTISE_AREA_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(length = LENGTH_LABEL)
    private String label;

    @Column(nullable = false, length = LENGTH_NAME)
    private String name;

    private String description;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "expertise_area2location", joinColumns = {
        @JoinColumn(name = "expertise_area_fk", nullable = false, updatable = false) },
        inverseJoinColumns = {
            @JoinColumn(name = "location_fk", nullable = false, updatable = false) })
    private Set<Location> locations = Sets.newHashSet();

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "expertise_area2location_level", joinColumns = {
        @JoinColumn(name = "expertise_area_fk", nullable = false, updatable = false) },
        inverseJoinColumns = {
            @JoinColumn(name = "location_level_fk", nullable = false, updatable = false) })
    private Set<LocationLevel> locationLevels = Sets.newHashSet();
}