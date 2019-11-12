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
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.metier.Metier;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@FieldNameConstants
@Entity
public class Operation implements IDataEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "OPERATION_SEQ")
    @SequenceGenerator(name = "OPERATION_SEQ", sequenceName="OPERATION_SEQ")
    private Integer id;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column(name="control_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date controlDate;

    @Column(name="qualification_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qualificationDate;

    @Column(name="qualification_comments", length = LENGTH_COMMENTS)
    private String qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = QualityFlag.class)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_fk", nullable = false)
    private Trip trip;

    @Column(name = "start_date_time", nullable = false)
    private Date startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private Date endDateTime;

    @Column(name = "fishing_start_date_time")
    private Date fishingStartDateTime;

    @Column(name = "fishing_end_date_time")
    private Date fishingEndDateTime;

    @Column(name = "rank_order_on_period")
    private Integer rankOrderOnPeriod;

    @Column(name = "has_catch")
    private Boolean hasCatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metier_fk")
    private Metier metier; // <-- /!\ metier is nullable !

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physical_gear_fk", nullable = false)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    private PhysicalGear physicalGear;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselPosition.class, mappedBy = VesselPosition.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselPosition> positions = new ArrayList<>();


    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselUseMeasurement.class, mappedBy = VesselUseMeasurement.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselUseMeasurement> vesselUseMeasurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = GearUseMeasurement.class, mappedBy = GearUseMeasurement.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<GearUseMeasurement> gearUseMeasurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sample.class, mappedBy = Sample.Fields.OPERATION)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
    private List<Sample> samples = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Batch.class, mappedBy = Batch.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Batch> batches;
}
