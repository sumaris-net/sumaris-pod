package net.sumaris.core.model.data.cruise;

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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.annotation.Comment;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.QualityFlag;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "scientific_cruise")
@NamedEntityGraph(
    name = ScientificCruise.GRAPH_PROGRAM_AND_MANAGER,
    attributeNodes = {
        @NamedAttributeNode(ScientificCruise.Fields.PROGRAM),
        @NamedAttributeNode(ScientificCruise.Fields.MANAGER_PERSON)
    }
)
public class ScientificCruise implements
    IRootDataEntity<Integer>,
    IWithRecorderPersonEntity<Integer, Person>,
    IWithVesselEntity<Integer, Vessel> {

    public static final String GRAPH_PROGRAM_AND_MANAGER = "ScientificCruise.programAndManager";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SCIENTIFIC_CRUISE_SEQ")
    @SequenceGenerator(name = "SCIENTIFIC_CRUISE_SEQ", sequenceName="SCIENTIFIC_CRUISE_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(length = 100, nullable = false)
    @Comment("Libellé décrivant la campagne")
    private String name;

    @Column(length = 255)
    @Comment("Référence SISMER de la campagne (ex: FI351997020020)")
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_person_fk", nullable = false)
    private Person managerPerson;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_person_fk")
    private Person recorderPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @Column(length = 2000)
    private String comments;

    @Column(name = "departure_date_time", nullable = false)
    private Date departureDateTime;

    @Column(name = "return_date_time", nullable = false)
    private Date returnDateTime;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Vessel.class)
    @JoinColumn(name = "vessel_fk", nullable = false)
    private Vessel vessel;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Program.class)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Trip.class, mappedBy = Trip.Fields.SCIENTIFIC_CRUISE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    @ToString.Exclude
    private List<Trip> trips;

    @Override
    public Date getControlDate() {
        Trip trip = getTrip();
        if (trip == null) return null;
        return trip.getControlDate();
    }

    @Override
    public void setControlDate(Date controlDate) {
        Trip trip = getOrCreateTrip();
        trip.setControlDate(controlDate);
    }

    @Override
    public Date getQualificationDate() {
        Trip trip = getTrip();
        if (trip == null) return null;
        return trip.getQualificationDate();
    }

    @Override
    public void setQualificationDate(Date qualificationDate) {
        Trip trip = getOrCreateTrip();
        trip.setQualificationDate(qualificationDate);
    }

    @Override
    public QualityFlag getQualityFlag() {
        Trip trip = getTrip();
        if (trip == null) return null;
        return trip.getQualityFlag();
    }

    @Override
    public void setQualityFlag(QualityFlag qualityFlag) {
        Trip trip = getOrCreateTrip();
        trip.setQualityFlag(qualityFlag);
    }

    @Override
    public String getQualificationComments() {
        Trip trip = getTrip();
        if (trip == null) return null;
        return trip.getQualificationComments();
    }

    @Override
    public void setQualificationComments(String qualificationComments) {
        Trip trip = getOrCreateTrip();
        trip.setQualificationComments(qualificationComments);
    }

    @Override
    public Date getValidationDate() {
        Trip trip = getTrip();
        if (trip == null) return null;
        return trip.getValidationDate();
    }

    @Override
    public void setValidationDate(Date validationDate) {
        Trip trip = getOrCreateTrip();
        trip.setValidationDate(validationDate);
    }

    public Trip getTrip() {
        if (CollectionUtils.isEmpty(trips)) return null;
        return trips.get(0);
    }

    public void setTrip(Trip trip) {
        if (trips == null) {
            trips = new ArrayList<>();
        }
        if (trips.size() > 0) {
            trips.set(0, trip);
        }
        else {
            trips.add(trip);
        }
    }

    protected Trip getOrCreateTrip() {
        Trip trip = getTrip();
        if (trip == null) {
            trip = new Trip();
            setTrip(trip);
        }
        return trip;
    }
}
