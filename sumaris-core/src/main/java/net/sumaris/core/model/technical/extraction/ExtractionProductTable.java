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
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Cacheable
@Table(name = "extraction_product_table")
public class ExtractionProductTable implements IItemReferentialEntity {

    public static final String PROPERTY_PRODUCT = "product";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EXTRACTION_PRODUCT_TABLE_SEQ")
    @SequenceGenerator(name = "EXTRACTION_PRODUCT_TABLE_SEQ", sequenceName="EXTRACTION_PRODUCT_TABLE_SEQ")
    private Integer id;

    @Column(nullable = false, length = LENGTH_LABEL)
    private String label;

    @Column(nullable = false, length = LENGTH_NAME)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(name="catalog", length = 30)
    private String catalog;

    @Column(name="schema", length = 30)
    private String schema;

    @Column(name="table_name", nullable = false, length = 30)
    private String tableName;

    @Column(name = "is_spatial")
    private Boolean isSpatial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_product_fk", nullable = false)
    private ExtractionProduct product;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ExtractionProductColumn.class, mappedBy = ExtractionProductColumn.PROPERTY_TABLE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ExtractionProductColumn> columns;

}
