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
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.UserProfile;
import net.sumaris.core.model.referential.location.Location;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;

import javax.persistence.*;
import java.util.*;

@FetchProfiles({
        @FetchProfile(name = "with-location",
            fetchOverrides = {
                @FetchProfile.FetchOverride(association = "location", entity = ObservedLocation.class, mode = FetchMode.JOIN),
                @FetchProfile.FetchOverride(association = "recorderDepartment", entity = ObservedLocation.class, mode = FetchMode.JOIN)
            })
})
@Data
@Entity
@Table(name="observed_location")
public class ObservedLocation implements IRootDataEntity<Integer>, IWithObserversEntityBean<Integer, Person> {

    public static final String PROPERTY_PROGRAM = "program";
    public static final String PROPERTY_START_DATE_TIME = "startDateTime";
    public static final String PROPERTY_END_DATE_TIME = "endDateTime";
    public static final String PROPERTY_LOCATION = "location";
    public static final String PROPERTY_SALES = "sales";
    public static final String PROPERTY_OBSERVERS = "observers";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "OBSERVED_LOCATION_SEQ")
    @SequenceGenerator(name = "OBSERVED_LOCATION_SEQ", sequenceName="OBSERVED_LOCATION_SEQ")
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

    @Column(name = "start_date_time", nullable = false)
    private Date startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private Date endDateTime;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = Location.class)
    @JoinColumn(name = "location_fk", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Program.class)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sale.class, mappedBy = Sale.PROPERTY_OBSERVED_LOCATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Sale> sales = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ObservedLocationMeasurement.class, mappedBy = ObservedLocationMeasurement.PROPERTY_OBSERVED_LOCATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ObservedLocationMeasurement> measurements = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = Person.class)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    @JoinTable(name = "observed_location2person", joinColumns = {
            @JoinColumn(name = "observed_location_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "person_fk", nullable = false, updatable = false) })
    private Set<Person> observers = Sets.newHashSet();

    public String toString() {
        return new StringBuilder().append("ObservedLocation(")
                .append("id=").append(id)
                .append(",dateTime=").append(startDateTime)
                .append(",location=").append(location)
                .append(")").toString();
    }

    public int hashCode() {
        return Objects.hash(id, program, startDateTime, location);
    }

}
