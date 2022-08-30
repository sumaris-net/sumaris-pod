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

import lombok.*;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.ITreeNodeEntity;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter

@FieldNameConstants
@Entity
public class Batch implements IDataEntity<Integer>,
    ITreeNodeEntity<Integer, Batch> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BATCH_SEQ")
    @SequenceGenerator(name = "BATCH_SEQ", sequenceName="BATCH_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
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

    @Column(name = "subgroup_count")
    private Integer subgroupCount;

    @Column(length = IDataEntity.LENGTH_COMMENTS)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxon_group_fk")
    private TaxonGroup taxonGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_taxon_fk")
    private ReferenceTaxon referenceTaxon;

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

    @Column(name="qualification_comments", length = LENGTH_COMMENTS)
    private String qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = QualityFlag.class)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    /* -- Tree link -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Batch.class, mappedBy = Fields.PARENT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Batch> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_batch_fk")
    private Batch parent;

    @Column(name = "hash")
    private Integer hash;

    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, mappedBy = BatchSortingMeasurement.Fields.BATCH)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<BatchSortingMeasurement> sortingMeasurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = BatchQuantificationMeasurement.Fields.BATCH)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<BatchQuantificationMeasurement> quantificationMeasurements = new ArrayList<>();

    /* -- parent entity -- */

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Operation.class)
    @JoinColumn(name = "operation_fk")
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Sale.class)
    @JoinColumn(name = "sale_fk")
    private Sale sale;

    // TODO: add location (for fishing area - need for sale)
    //@ManyToOne(fetch = FetchType.LAZY, targetEntity = Location.class)
    //@JoinColumn(name = "location_fk")
    //private Location location;

}
