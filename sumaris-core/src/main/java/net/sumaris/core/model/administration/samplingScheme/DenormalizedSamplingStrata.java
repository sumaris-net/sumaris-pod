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

package net.sumaris.core.model.administration.samplingScheme;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.location.Location;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "denormalized_sampling_strata")
public class DenormalizedSamplingStrata implements IItemReferentialEntity<Integer> {

    public static final String ENTITY_NAME = "DenormalizedSamplingStrata";

    @Id
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_LABEL)
    private String label;

    @Column(length = IItemReferentialEntity.LENGTH_NAME)
    private String name;

    @Column(name = "start_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "end_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    private String description;

    @Column(length = IDataEntity.LENGTH_COMMENTS)
    private String comments;

    @Column(length = 100)
    private String observationLocationComments;

    @Column(length = 100)
    private String samplingStrategy;

    @Column(length = 100)
    private String taxonGroupName;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_LABEL)
    private String samplingSchemeLabel;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_NAME)
    private String samplingSchemeName;

    private String samplingSchemeDescription;

    @Column(length = IItemReferentialEntity.LENGTH_NAME)
    private String gearMeshRange;

    @Column(length = IItemReferentialEntity.LENGTH_NAME)
    private String vesselLengthRange;

    private String metier;

    @Column(length = 150)
    private String areaName;

    private String subAreaLocationIds;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_fk")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_fk")
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observation_location_fk")
    private Location observationLocation;
}
