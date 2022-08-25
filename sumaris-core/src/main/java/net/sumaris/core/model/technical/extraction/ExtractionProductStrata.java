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

package net.sumaris.core.model.technical.extraction;

import lombok.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;

import javax.persistence.*;
import java.util.Date;

@Getter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Cacheable
@Table(name = "extraction_product_strata")
public class ExtractionProductStrata implements IItemReferentialEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EXTRACTION_PRODUCT_STRATA_SEQ")
    @SequenceGenerator(name = "EXTRACTION_PRODUCT_STRATA_SEQ", sequenceName="EXTRACTION_PRODUCT_STRATA_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @ToString.Include
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_LABEL)
    private String label;

    @Column(length = IItemReferentialEntity.LENGTH_NAME)
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

    @Column(name = "is_default")
    private Boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_product_fk", nullable = false)
    private ExtractionProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_table_fk")
    private ExtractionProductTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_extraction_column_fk")
    private ExtractionProductColumn timeColumn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_extraction_column_fk")
    private ExtractionProductColumn spaceColumn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agg_extraction_column_fk")
    private ExtractionProductColumn aggColumn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_extraction_column_fk")
    private ExtractionProductColumn techColumn;

    @Column(name = "agg_function", length = 30)
    private String aggFunction;

}
