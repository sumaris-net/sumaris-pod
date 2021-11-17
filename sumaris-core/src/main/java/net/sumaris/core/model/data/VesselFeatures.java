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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Formula;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@FieldNameConstants
@Entity
@Table(name = "vessel_features")
public class VesselFeatures implements IDataEntity<Integer>,
        IWithRecorderPersonEntity<Integer, Person>,
        IWithRecorderDepartmentEntity<Integer, Department>,
        IWithVesselEntity<Integer, Vessel>{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "VESSEL_FEATURES_SEQ")
    @SequenceGenerator(name = "VESSEL_FEATURES_SEQ", sequenceName="VESSEL_FEATURES_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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

    @Column(length = LENGTH_COMMENTS)
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

    @Column(name = "start_date", nullable = false)
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

    @Formula("coalesce(end_date, date'2100-01-01')")
    @Column(name = "nvl_end_date", insertable = false, updatable = false)
    private Date nvlEndDate;

    private String name;

    @Column(name = "exterior_marking")
    private String exteriorMarking;

    @Column(name = "length_over_all")
    private Integer lengthOverAll;

    @Column(name = "administrative_power")
    private Integer administrativePower;

    @Column(name = "gross_tonnage_grt")
    private Integer grossTonnageGrt;

    @Column(name = "gross_tonnage_gt")
    private Integer grossTonnageGt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="base_port_location_fk", nullable = false)
    private Location basePortLocation;

    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselPhysicalMeasurement.class, mappedBy = VesselPhysicalMeasurement.Fields.VESSEL_FEATURES)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselPhysicalMeasurement> measurements = new ArrayList<>();
}
