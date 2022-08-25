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
import lombok.*;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@FieldNameConstants
@Entity
@NamedEntityGraph(
    name = Landing.GRAPH_LOCATION_AND_PROGRAM,
    attributeNodes = {
        @NamedAttributeNode(Landing.Fields.LOCATION),
        @NamedAttributeNode(Landing.Fields.PROGRAM)
    }
)
public class Landing implements IRootDataEntity<Integer>,
        IWithObserversEntity<Integer, Person>,
        IWithVesselEntity<Integer, Vessel>,
        IWithSamplesEntity<Integer, Sample>,
        IWithProductsEntity<Integer, Product> {

    public static final String GRAPH_LOCATION_AND_PROGRAM = "Landing.locationAndProgram";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LANDING_SEQ")
    @SequenceGenerator(name = "LANDING_SEQ", sequenceName="LANDING_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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

    @Column(name = "landing_date_time", nullable = false)
    private Date dateTime;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = Location.class)
    @JoinColumn(name = "landing_location_fk", nullable = false)
    private Location location;

    @Column(name = "rank_order")
    private Integer rankOrder;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Program.class)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = Person.class)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    @JoinTable(name = "landing2observer_person", joinColumns = {
            @JoinColumn(name = "landing_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "person_fk", nullable = false, updatable = false) })
    private Set<Person> observers = Sets.newHashSet();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sample.class, mappedBy = Sample.Fields.LANDING)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Sample> samples = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Product.class, mappedBy = Product.Fields.LANDING)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Product> products = new ArrayList<>();

    // FIXME when enable, LandginServiceImpl.findAllByObservedLocationId() always fetch expectedSale, but don't knwn shy
    // (see issue sumaris-pod #24)
    /*@OneToMany(fetch = FetchType.LAZY, targetEntity = ExpectedSale.class, mappedBy = ExpectedSale.Fields.LANDING)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ExpectedSale> expectedSales = new ArrayList<>();*/

    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = LandingMeasurement.class, mappedBy = LandingMeasurement.Fields.LANDING)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<LandingMeasurement> landingMeasurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = SurveyMeasurement.class, mappedBy = SurveyMeasurement.Fields.LANDING)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<SurveyMeasurement> surveyMeasurements = new ArrayList<>();

    /* -- parent -- */

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ObservedLocation.class)
    @JoinColumn(name = "observed_location_fk")
    @ToString.Exclude
    private ObservedLocation observedLocation;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Trip.class)
    @JoinColumn(name = "trip_fk")
    @ToString.Exclude
    private Trip trip;
}
