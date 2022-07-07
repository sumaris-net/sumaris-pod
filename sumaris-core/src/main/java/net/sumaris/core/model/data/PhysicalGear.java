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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.ITreeNodeEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.QualityFlag;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name="physical_gear")
public class PhysicalGear implements IRootDataEntity<Integer>,
    ITreeNodeEntity<Integer, PhysicalGear> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PHYSICAL_GEAR_SEQ")
    @SequenceGenerator(name = "PHYSICAL_GEAR_SEQ", sequenceName="PHYSICAL_GEAR_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @ToString.Include
    private Integer id;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Gear.class)
    @JoinColumn(name = "gear_fk", nullable = false)
    @ToString.Include
    private Gear gear;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column
    private Integer hash;

    /* -- Quality insurance -- */

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_person_fk")
    private Person recorderPerson;

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


    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Program.class)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    /* -- Tree link -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sample.class, mappedBy = PhysicalGear.Fields.PARENT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<PhysicalGear> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_physical_gear_fk")
    private PhysicalGear parent;

    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = PhysicalGearMeasurement.class, mappedBy = PhysicalGearMeasurement.Fields.PHYSICAL_GEAR)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<PhysicalGearMeasurement> measurements = new ArrayList<>();

    /* -- Owner entities -- */

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Trip.class)
    @JoinColumn(name = "trip_fk", nullable = false)
    private Trip trip;

    public String toString() {
        return String.format("PhysicalGear{id=%s,rankOrder=%s}", id, rankOrder);
    }
}
