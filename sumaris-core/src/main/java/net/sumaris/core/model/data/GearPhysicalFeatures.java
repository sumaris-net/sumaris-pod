
/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2024 SUMARiS Consortium
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

package net.sumaris.core.model.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.metier.Metier;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "gear_physical_features")
public class GearPhysicalFeatures implements IUseFeaturesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GEAR_PHYSICAL_FEATURES_SEQ")
    @SequenceGenerator(name = "GEAR_PHYSICAL_FEATURES_SEQ", sequenceName="GEAR_PHYSICAL_FEATURES_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @Column(name="rank_order", nullable = false)
    private Short rankOrder;

    @Column(name="start_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name="end_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "metier_fk")
    private Metier metier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gear_fk", nullable = false)
    private Gear gear;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "other_gear_fk")
    private Gear otherGear;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column(name = "control_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date controlDate;

    @Column(name = "validation_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date validationDate;

    @Column(name = "qualification_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qualificationDate;

    @Column(name = "qualification_comments", length = LENGTH_COMMENTS)
    private String qualificationComments;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vessel_fk", nullable = false)
    private Vessel vessel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = GearPhysicalMeasurement.Fields.GEAR_PHYSICAL_FEATURES, cascade = CascadeType.REMOVE)
    private List<GearPhysicalMeasurement> measurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = GearPhysicalFeaturesOrigin.class, mappedBy = GearPhysicalFeaturesOrigin.Fields.GEAR_PHYSICAL_FEATURES, cascade = CascadeType.REMOVE)
    private List<GearPhysicalFeaturesOrigin> origins;

    @Transient
    private Department recorderDepartment; // Missing in DB, but expected by IDataEntity

    /* -- parent entity -- */

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ActivityCalendar.class )
    @JoinColumn(name = "activity_calendar_fk")
    @ToString.Exclude
    private ActivityCalendar activityCalendar;

}
