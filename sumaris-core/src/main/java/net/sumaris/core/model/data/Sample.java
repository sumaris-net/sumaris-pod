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
import net.sumaris.core.model.referential.pmfm.Matrix;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@FieldNameConstants
@Entity
@Table(name = "sample")
public class Sample implements IRootDataEntity<Integer>,
    ITreeNodeEntity<Integer, Sample> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SAMPLE_SEQ")
    @SequenceGenerator(name = "SAMPLE_SEQ", sequenceName="SAMPLE_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(length = 40, nullable = false)
    private String label;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "sample_date")
    private Date sampleDate;

    @Column(name = "individual_count")
    private Integer individualCount;

    @Column(name = "sample_size")
    private Double size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_unit_fk")
    private Unit sizeUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxon_group_fk")
    private TaxonGroup taxonGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_taxon_fk")
    private ReferenceTaxon referenceTaxon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matrix_fk", nullable = false)
    private Matrix matrix;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column(name = "hash")
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

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sample.class, mappedBy = Fields.PARENT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    @ToString.Exclude
    private List<Sample> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_sample_fk")
    @ToString.Exclude
    private Sample parent;


    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = SampleMeasurement.class, mappedBy = SampleMeasurement.Fields.SAMPLE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<SampleMeasurement> measurements = new ArrayList<>();

    /* -- Parent link -- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_fk")
    @ToString.Exclude
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_fk")
    @ToString.Exclude
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landing_fk")
    @ToString.Exclude
    private Landing landing;

    public String toString() {
        return String.format("Sample{id=%s,label=%s}", id, label);
    }
}
