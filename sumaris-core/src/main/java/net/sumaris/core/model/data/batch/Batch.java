package net.sumaris.core.model.data.batch;

import lombok.Data;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
public class Batch implements IDataEntity<Integer> {

    public static final String PROPERTY_RANK_ORDER = "rankOrder";
    public static final String PROPERTY_PARENT = "parent";
    public static final String PROPERTY_SORTING_MEASUREMENTS = "sortingMeasurements";
    public static final String PROPERTY_QUANTIFICATION_MEASUREMENTS = "quantificationMeasurements";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(length = 40)
    private String label;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name="exhaustive_inventory")
    private Boolean exhaustiveInventory;

    @Column(name = "sampling_ratio")
    private Double samplingRatio;

    @Column(name = "sampling_ratio_text", length = 50)
    private String samplingRatioText;

    @Column(name = "individual_count")
    private Integer individualCount;

    @Column(length = 2000)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxon_group_fk")
    private TaxonGroup taxonGroup;

    /* -- quality insurance -- */

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @Column(name="control_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date controlDate;

    @Column(name="qualification_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qualificationDate;

    @Column(name="qualification_comments", length = 2000)
    private Date qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    /* -- Tree link -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Batch.class, mappedBy = PROPERTY_PARENT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Batch> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_batch_fk")
    private Batch parent;

    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = BatchSortingMeasurement.class, mappedBy = BatchSortingMeasurement.PROPERTY_BATCH)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<BatchSortingMeasurement> sortingMeasurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = BatchQuantitifcationMeasurement.class, mappedBy = BatchQuantitifcationMeasurement.PROPERTY_BATCH)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<BatchQuantitifcationMeasurement> quantificationMeasurements = new ArrayList<>();

    /* -- parent entity -- */

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Operation.class)
    @JoinColumn(name = "operation_fk", nullable = false)
    private Operation operation;
}
