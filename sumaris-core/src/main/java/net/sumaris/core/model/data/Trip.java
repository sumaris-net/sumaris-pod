package net.sumaris.core.model.data;

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

import com.google.common.collect.Sets;
import lombok.Data;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.QualityFlag;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;

import javax.persistence.*;
import java.util.*;

@FetchProfiles({
        @FetchProfile(name = Trip.FETCH_PROFILE_LOCATION,
            fetchOverrides = {
                @FetchProfile.FetchOverride(association = "departureLocation", entity = Trip.class, mode = FetchMode.JOIN),
                @FetchProfile.FetchOverride(association = "returnLocation", entity = Trip.class, mode = FetchMode.JOIN)
            }),
        @FetchProfile(name = Trip.FETCH_PROFILE_RECORDER,
                fetchOverrides = {
                        @FetchProfile.FetchOverride(association = IRootDataEntity.PROPERTY_RECORDER_DEPARTMENT, entity = Trip.class, mode = FetchMode.JOIN),
                        @FetchProfile.FetchOverride(association = IRootDataEntity.PROPERTY_RECORDER_PERSON, entity = Trip.class, mode = FetchMode.JOIN)
                }),
        @FetchProfile(name = Trip.FETCH_PROFILE_OBSERVERS,
                fetchOverrides = {
                        @FetchProfile.FetchOverride(association = IWithObserversEntity.PROPERTY_OBSERVERS, entity = Trip.class, mode = FetchMode.JOIN)
                })
})
@Data
@Entity
public class Trip implements IRootDataEntity<Integer>,
        IWithObserversEntity<Integer, Person>,
        IWithVesselEntity<Integer, Vessel> {

    public static final String FETCH_PROFILE_LOCATION  = "trip-location";
    public static final String FETCH_PROFILE_RECORDER  = "trip-recorder";
    public static final String FETCH_PROFILE_OBSERVERS = "trip-observers";

    public static final String PROPERTY_PROGRAM = "program";
    public static final String PROPERTY_DEPARTURE_DATE_TIME = "departureDateTime";
    public static final String PROPERTY_RETURN_DATE_TIME = "returnDateTime";
    public static final String PROPERTY_DEPARTURE_LOCATION = "departureLocation";
    public static final String PROPERTY_RETURN_LOCATION = "returnLocation";
    public static final String PROPERTY_VESSEL = "vessel";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "TRIP_SEQ")
    @SequenceGenerator(name = "TRIP_SEQ", sequenceName="TRIP_SEQ")
    private Integer id;

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

    @Column(name="control_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date controlDate;

    @Column(name="validation_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date validationDate;

    @Column(name="qualification_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qualificationDate;

    @Column(name="qualification_comments", length = LENGTH_COMMENTS)
    private String qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = QualityFlag.class)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Vessel.class)
    @JoinColumn(name = "vessel_fk", nullable = false)
    private Vessel vessel;

    @Column(name = "departure_date_time", nullable = false)
    private Date departureDateTime;

    @Column(name = "return_date_time", nullable = false)
    private Date returnDateTime;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Location.class)
    @JoinColumn(name = "departure_location_fk", nullable = false)
    private Location departureLocation;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Location.class)
    @JoinColumn(name = "return_location_fk", nullable = false)
    private Location returnLocation;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = Program.class)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Operation.class, mappedBy = Operation.PROPERTY_TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Operation> operations = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = PhysicalGear.class, mappedBy = PhysicalGear.PROPERTY_TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    @OrderBy("rankOrder ASC")
    private List<PhysicalGear> physicalGears = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sale.class, mappedBy = Sale.PROPERTY_TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Sale> sales = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselUseMeasurement.class, mappedBy = VesselUseMeasurement.PROPERTY_TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselUseMeasurement> measurements = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = Person.class)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    @JoinTable(name = "trip2observer_person", joinColumns = {
            @JoinColumn(name = "trip_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "person_fk", nullable = false, updatable = false) })
    private Set<Person> observers = Sets.newHashSet();

    public String toString() {
        return new StringBuilder().append("Trip(")
                .append("id=").append(id)
                .append(",departureDateTime=").append(departureDateTime)
                .append(")").toString();
    }

    public int hashCode() {
        return Objects.hash(id, vessel, program, departureDateTime);
    }

}
