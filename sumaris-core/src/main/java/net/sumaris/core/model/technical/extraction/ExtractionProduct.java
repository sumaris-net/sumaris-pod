package net.sumaris.core.model.technical.extraction;

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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.util.Beans;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.SortNatural;

import javax.persistence.*;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@FieldNameConstants
@Entity
@Cacheable
@Table(name = "extraction_product")
@EqualsAndHashCode
public class ExtractionProduct implements IItemReferentialEntity,
        IExtractionFormat,
        IWithRecorderPersonEntity<Integer, Person>,
        IWithRecorderDepartmentEntity<Integer, Department> {

    public final static int LENGTH_VERSION = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EXTRACTION_PRODUCT_SEQ")
    @SequenceGenerator(name = "EXTRACTION_PRODUCT_SEQ", sequenceName="EXTRACTION_PRODUCT_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Exclude
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(nullable = false, length = LENGTH_LABEL)
    private String label;

    @Column(nullable = false, length = LENGTH_NAME)
    private String name;

    private String description;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column(length = LENGTH_LABEL)
    private String format;

    @Column(length = 10)
    private String version;

    @Lob
    private String documentation;

    @Lob
    private String filter;

    @Column(name = "is_spatial")
    private Boolean isSpatial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_person_fk")
    private Person recorderPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_extraction_product_fk")
    @ToString.Exclude
    private ExtractionProduct parent;

    @OneToMany(fetch = FetchType.EAGER, targetEntity = ExtractionProductStrata.class, mappedBy = ExtractionProductStrata.Fields.PRODUCT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ExtractionProductStrata> stratum;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ExtractionProductTable.class, mappedBy = ExtractionProductTable.Fields.PRODUCT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    @OrderBy(ExtractionProductTable.Fields.RANK_ORDER + " ASC")
    @SortNatural
    private List<ExtractionProductTable> tables = new ArrayList<>();

    @Override
    public ExtractionCategoryEnum getCategory() {
        return ExtractionCategoryEnum.PRODUCT;
    }

    @Override
    public String[] getSheetNames() {
        if (tables == null) return null;
        return tables.stream().map(ExtractionProductTable::getLabel).toArray(String[]::new);
    }
}
