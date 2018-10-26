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

import lombok.Data;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.measure.VesselUseMeasurement;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.Location;
import net.sumaris.core.model.referential.QualityFlag;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@FetchProfiles({
        @FetchProfile(name = "with-location",
            fetchOverrides = {
                @FetchProfile.FetchOverride(association = "departureLocation", entity = Trip.class, mode = FetchMode.JOIN),
                @FetchProfile.FetchOverride(association = "returnLocation", entity = Trip.class, mode = FetchMode.JOIN),
                    @FetchProfile.FetchOverride(association = "recorderDepartment", entity = Trip.class, mode = FetchMode.JOIN)
            })
})
@Data
@Entity
public class Trip implements IRootDataEntity<Integer> {

    public static final String PROPERTY_PROGRAM = "program";
    public static final String PROPERTY_DEPARTURE_DATE_TIME = "departureDateTime";
    public static final String PROPERTY_RETURN_DATE_TIME = "returnDateTime";
    public static final String PROPERTY_DEPARTURE_LOCATION = "departureLocation";
    public static final String PROPERTY_RETURN_LOCATION = "returnLocation";
    public static final String PROPERTY_VESSEL = "vessel";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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

    @Column(name="qualification_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qualificationDate;

    @Column(name="qualification_comments", length = LENGTH_COMMENTS)
    private Date qualificationComments;

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

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = Location.class)
    @JoinColumn(name = "departure_location_fk", nullable = false)
    private Location departureLocation;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = Location.class)
    @JoinColumn(name = "return_location_fk", nullable = false)
    private Location returnLocation;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Program.class)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Operation.class, mappedBy = Operation.PROPERTY_TRIP)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
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

    public String toString() {
        return new StringBuilder().append("Trip(")
                .append("id=").append(id)
                .append(",departureDateTime=").append(departureDateTime)
                .append(")").toString();
    }
}
