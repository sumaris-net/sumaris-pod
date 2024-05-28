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

import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.grouping.Grouping;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.regulation.Fishery;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "sampling_strata")
@Cacheable
public class SamplingStrata implements IItemReferentialEntity<Integer> {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SAMPLING_STRATA_SEQ")
    @SequenceGenerator(name = "SAMPLING_STRATA_SEQ", sequenceName="SAMPLING_STRATA_SEQ", allocationSize = IItemReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_LABEL)
    private String label;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_NAME)
    private String name;

    private String description;

    @Column(name = "start_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "end_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_fk")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_fk")
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observation_location_fk")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @OneToMany(mappedBy = SamplingStrataMeasurement.Fields.SAMPLING_STRATA, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SamplingStrataMeasurement> samplingStrataMeasurements;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(name = "sampling_strata2grouping", joinColumns = {
        @JoinColumn(name = "sampling_strata_fk", nullable = false, updatable = false) },
        inverseJoinColumns = {
            @JoinColumn(name = "grouping_fk", nullable = false, updatable = false) })
    private Set<Grouping> groupings;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(name = "sampling_strata2fishery", joinColumns = {
        @JoinColumn(name = "sampling_strata_fk", nullable = false, updatable = false) },
        inverseJoinColumns = {
            @JoinColumn(name = "fishery_fk", nullable = false, updatable = false) })
    private Set<Fishery> fisheries = Sets.newHashSet();


//    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
//    @JoinTable(name = "sampling_strata2vessel", joinColumns = {
//        @JoinColumn(name = "sampling_strata_fk", nullable = false, updatable = false) },
//        inverseJoinColumns = {
//            @JoinColumn(name = "vessel_fk", nullable = false, updatable = false) })
//    private Set<Vessel> vessels = Sets.newHashSet();

    /* -- parent -- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sampling_scheme_fk", nullable = false)
    private SamplingScheme samplingScheme;

}
