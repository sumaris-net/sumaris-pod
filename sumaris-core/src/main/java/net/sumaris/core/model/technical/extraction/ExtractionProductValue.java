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
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "extraction_product_value")
public class ExtractionProductValue implements IEntity<Integer> {

    public static final String PROPERTY_COLUMN = "column";
    public static final String PROPERTY_PARENT = "parent";
    public static final String PROPERTY_LABEL = "label";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EXTRACTION_PRODUCT_VALUE_SEQ")
    @SequenceGenerator(name = "EXTRACTION_PRODUCT_VALUE_SEQ", sequenceName="EXTRACTION_PRODUCT_VALUE_SEQ")
    private Integer id;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_LABEL)
    private String label;

    @Column(length = IItemReferentialEntity.LENGTH_NAME)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_product_column_fk", nullable = false)
    private ExtractionProductColumn column;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_fk")
    private ExtractionProductValue parent;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ExtractionProductValue.class, mappedBy = PROPERTY_PARENT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ExtractionProductValue> children;

}
