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

import lombok.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IWithValidityStatusEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.ValidityStatus;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "location", indexes = {
    @Index(name = "location_label_idx", columnList = "label")
})
@Cacheable
public class Location implements IItemReferentialEntity<Integer>, IWithValidityStatusEntity<Integer, ValidityStatus> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LOCATION_SEQ")
    @SequenceGenerator(name = "LOCATION_SEQ", sequenceName="LOCATION_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
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

    @Column(nullable = false, length = LENGTH_LABEL)
    private String label;

    @Column(nullable = false, length = LENGTH_NAME)
    private String name;

    private String description;

    private Double bathymetry;

    @Column(name = "ut_format")
    private Short utFormat;

    @Column(name = "daylight_saving_time")
    private Boolean daylightSavingTime;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = LocationLevel.class)
    @JoinColumn(name = "location_level_fk", nullable = false)
    private LocationLevel locationLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validity_status_fk", nullable = false)
    private ValidityStatus validityStatus;
}
