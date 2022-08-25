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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.nuiton.i18n.I18n;

import javax.persistence.*;
import java.util.*;

@FetchProfiles({
        @FetchProfile(name = "with-location",
            fetchOverrides = {
                @FetchProfile.FetchOverride(association = "location", entity = ObservedLocation.class, mode = FetchMode.JOIN),
                @FetchProfile.FetchOverride(association = "recorderDepartment", entity = ObservedLocation.class, mode = FetchMode.JOIN)
            })
})
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name="observed_location")
@NamedEntityGraph(
    name = ObservedLocation.GRAPH_LOCATION_AND_PROGRAM,
    attributeNodes = {
        @NamedAttributeNode(ObservedLocation.Fields.LOCATION),
        @NamedAttributeNode(ObservedLocation.Fields.PROGRAM)
    }
)
public class ObservedLocation implements IRootDataEntity<Integer>, IWithObserversEntity<Integer, Person> {

    public static final String GRAPH_LOCATION_AND_PROGRAM = "ObservedLocation.locationWithProgram";
    static {
        I18n.n("sumaris.persistence.table.observedLocation");
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "OBSERVED_LOCATION_SEQ")
    @SequenceGenerator(name = "OBSERVED_LOCATION_SEQ", sequenceName="OBSERVED_LOCATION_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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

    @Column(name = "start_date_time", nullable = false)
    @ToString.Include
    private Date startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private Date endDateTime;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = Location.class)
    @JoinColumn(name = "location_fk", nullable = false)
    @ToString.Include
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Program.class)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sale.class, mappedBy = Sale.Fields.OBSERVED_LOCATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Sale> sales = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Landing.class, mappedBy = Landing.Fields.OBSERVED_LOCATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Landing> landings = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ObservedLocationMeasurement.class, mappedBy = ObservedLocationMeasurement.Fields.OBSERVED_LOCATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ObservedLocationMeasurement> measurements = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = Person.class)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    @JoinTable(name = "observed_location2person", joinColumns = {
            @JoinColumn(name = "observed_location_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "person_fk", nullable = false, updatable = false) })
    private Set<Person> observers = Sets.newHashSet();

    public int hashCode() {
        return Objects.hash(id, program, startDateTime, location);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ObservedLocation that = (ObservedLocation) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .isEquals();
    }
}
