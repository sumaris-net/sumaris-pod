/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.QualityFlag;
import org.hibernate.annotations.Cascade;
import org.nuiton.i18n.I18n;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "activity_calendar", uniqueConstraints = {
    @UniqueConstraint(
        columnNames = {"vessel_fk", "program_fk", "year"},
        name = "activity_calendar_unique_key"
    )
})
@NamedEntityGraph(
    name = ActivityCalendar.GRAPH_PROGRAM,
    attributeNodes = {
        @NamedAttributeNode(ActivityCalendar.Fields.PROGRAM)
    }
)
public class ActivityCalendar implements IRootDataEntity<Integer>,
    IWithVesselEntity<Integer, Vessel>,
    IWithVesselUseFeaturesEntity<Integer, VesselUseFeatures>,
    IWithGearUseFeaturesEntity<Integer, GearUseFeatures> {

    static {
        I18n.n("sumaris.persistence.table.activityCalendar");
    }

    public static final String GRAPH_PROGRAM = "ActivityCalendar.withProgram";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ACTIVITY_CALENDAR_SEQ")
    @SequenceGenerator(name = "ACTIVITY_CALENDAR_SEQ", sequenceName="ACTIVITY_CALENDAR_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "direct_survey_investigation", nullable = false)
    private Boolean directSurveyInvestigation;

    @Column(name = "economic_survey")
    private Boolean economicSurvey;

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
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vessel_fk", nullable = false)
    private Vessel vessel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_person_fk")
    private Person recorderPerson;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = SurveyMeasurement.Fields.ACTIVITY_CALENDAR)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<SurveyMeasurement> surveyMeasurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = VesselUseFeatures.Fields.ACTIVITY_CALENDAR)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselUseFeatures> vesselUseFeatures = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = GearUseFeatures.Fields.ACTIVITY_CALENDAR)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<GearUseFeatures> gearUseFeatures = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = GearPhysicalFeatures.Fields.ACTIVITY_CALENDAR)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<GearPhysicalFeatures> gearPhysicalFeatures = new ArrayList<>();

}
