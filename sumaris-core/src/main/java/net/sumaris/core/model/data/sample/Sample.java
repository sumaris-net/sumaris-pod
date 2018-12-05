package net.sumaris.core.model.data.sample;

import lombok.Data;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.data.batch.Batch;
import net.sumaris.core.model.referential.Matrix;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
public class Sample implements IRootDataEntity<Integer> {


    public static final String PROPERTY_OPERATION = "operation";
    public static final String PROPERTY_PARENT = "parent";
    public static final String PROPERTY_SAMPLE_MEASUREMENTS = "sampleMeasurements";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(length = 40, nullable = false)
    private String label;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "sample_date")
    private Date sampleDate;

    @Column(name = "individual_count")
    private Integer individualCount;

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
    private Date qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = QualityFlag.class)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    /* -- Tree link -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sample.class, mappedBy = PROPERTY_PARENT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Sample> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_sample_fk")
    private Sample parent;

    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = SampleMeasurement.class, mappedBy = SampleMeasurement.PROPERTY_SAMPLE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<SampleMeasurement> sampleMeasurements = new ArrayList<>();

    /* -- Parent link -- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_fk", nullable = false)
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_fk")
    private Batch batch;
}
