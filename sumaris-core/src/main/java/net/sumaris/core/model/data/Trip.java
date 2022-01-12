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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.QualityFlag;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cascade;
import org.nuiton.i18n.I18n;

import javax.persistence.*;
import java.util.*;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "trip")
@NamedEntityGraph(
    name = Trip.GRAPH_LOCATIONS_AND_PROGRAM,
    attributeNodes = {
        @NamedAttributeNode(Trip.Fields.DEPARTURE_LOCATION),
        @NamedAttributeNode(Trip.Fields.RETURN_LOCATION),
        @NamedAttributeNode(Trip.Fields.PROGRAM)
    }
)
public class Trip implements IRootDataEntity<Integer>,
        IWithObserversEntity<Integer, Person>,
        IWithVesselEntity<Integer, Vessel> {

    static {
        I18n.n("sumaris.persistence.table.trip");
    }

    public static final String GRAPH_LOCATIONS_AND_PROGRAM = "Trip.locationsWithProgram";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRIP_SEQ")
    @SequenceGenerator(name = "TRIP_SEQ", sequenceName="TRIP_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @ToString.Include
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
    @ToString.Include
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

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Operation.class, mappedBy = Operation.Fields.TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Operation> operations = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = PhysicalGear.class, mappedBy = PhysicalGear.Fields.TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    @OrderBy("rankOrder ASC")
    private List<PhysicalGear> physicalGears = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sale.class, mappedBy = Sale.Fields.TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Sale> sales = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ExpectedSale.class, mappedBy = ExpectedSale.Fields.TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ExpectedSale> expectedSales = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Landing.class, mappedBy = Landing.Fields.TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Landing> landings;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselUseMeasurement.class, mappedBy = VesselUseMeasurement.Fields.TRIP)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselUseMeasurement> measurements = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = Person.class)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    @JoinTable(name = "trip2observer_person", joinColumns = {
            @JoinColumn(name = "trip_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "person_fk", nullable = false, updatable = false) })
    private Set<Person> observers = Sets.newHashSet();

    public int hashCode() {
        return Objects.hash(id, vessel, program, departureDateTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Trip that = (Trip) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .isEquals();
    }
}
