package net.sumaris.core.model.referential.location;

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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;

import javax.persistence.*;
import java.util.Date;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "location_level")
@Cacheable
public class LocationLevel implements IItemReferentialEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LOCATION_LEVEL_SEQ")
    @SequenceGenerator(name = "LOCATION_LEVEL_SEQ", sequenceName="LOCATION_LEVEL_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @ToString.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_LABEL)
    @ToString.Include
    private String label;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_NAME)
    private String name;

    private String description;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = LocationLevel.class)
    @JoinColumn(name = "parent_location_level_fk")
    private LocationLevel parent;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = LocationClassification.class)
    @JoinColumn(name = "location_classification_fk") // TODO should be not nullable
    private LocationClassification locationClassification;
}
