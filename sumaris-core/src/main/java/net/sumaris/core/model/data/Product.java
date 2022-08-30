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
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.pmfm.Method;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "product")
public class Product
    implements IDataEntity<Integer>, IWithRecorderPersonEntity<Integer, Person>,
        IWithDataQualityEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PRODUCT_SEQ")
    @SequenceGenerator(name = "PRODUCT_SEQ", sequenceName="PRODUCT_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(length = 40)
    private String label;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "individual_count")
    private Integer individualCount;

    @Column(name = "subgroup_count")
    private Double subgroupCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxon_group_fk")
    private TaxonGroup taxonGroup;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    /* -- Quality insurance -- */

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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SaleType.class)
    @JoinColumn(name = "sale_type_fk")
    private SaleType saleType;

    private Double weight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weight_method_fk")
    private Method weightMethod;

    private Double cost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dressing_fk")
    private QualitativeValue dressing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preservation_fk")
    private QualitativeValue preservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_category_fk")
    private QualitativeValue sizeCategory;

    /* -- measurements -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ProductSortingMeasurement.class, mappedBy = ProductSortingMeasurement.Fields.PRODUCT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ProductSortingMeasurement> sortingMeasurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ProductQuantificationMeasurement.class, mappedBy = ProductQuantificationMeasurement.Fields.PRODUCT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ProductQuantificationMeasurement> quantificationMeasurements = new ArrayList<>();

    /* -- Parent link -- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_fk")
    @ToString.Exclude
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landing_fk")
    @ToString.Exclude
    private Landing landing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_fk")
    @ToString.Exclude
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expected_sale_fk")
    @ToString.Exclude
    private ExpectedSale expectedSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_fk")
    @ToString.Exclude
    private Batch batch;
}
