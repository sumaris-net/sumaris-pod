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

import lombok.*;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.VesselType;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter

@FieldNameConstants
@Entity
@Table(name = "vessel")
/**
 * A vessel is fishing vessel or scientific vessel, or any halieutic resources user
 */
public class Vessel implements IRootDataEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "VESSEL_SEQ")
    @SequenceGenerator(name = "VESSEL_SEQ", sequenceName="VESSEL_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = VesselType.class)
    @JoinColumn(name="vessel_type_fk", nullable = false)
    private VesselType vesselType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Program.class)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselFeatures.class, mappedBy = VesselFeatures.Fields.VESSEL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselFeatures> vesselFeatures = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselRegistrationPeriod.class, mappedBy = VesselRegistrationPeriod.Fields.VESSEL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselRegistrationPeriod> vesselRegistrationPeriods = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselOwnerPeriod.class, mappedBy = VesselOwnerPeriod.Fields.VESSEL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselOwnerPeriod> vesselOwnerPeriods = new ArrayList<>();

}
