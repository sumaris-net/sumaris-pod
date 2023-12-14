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

package net.sumaris.core.model.referential.regulation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.model.referential.taxon.TaxonGroup;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "minimum_size_allowed")
public class MinimumSizeAllowed {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MINIMUM_SIZE_ALLOWED_SEQ")
    @SequenceGenerator(name = "MINIMUM_SIZE_ALLOWED_SEQ", sequenceName="MINIMUM_SIZE_ALLOWED_SEQ", allocationSize = IItemReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_unit_fk")
    private Unit sizeUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxon_group_fk")
    private TaxonGroup taxonGroup;

    @ManyToMany
    @JoinTable(
        name = "minimum_size_allowed2location",
        joinColumns = @JoinColumn(name = "minimum_size_allowed_fk"),
        inverseJoinColumns = @JoinColumn(name = "location_fk")
    )
    private Set<Location> locations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corpus_fk")
    private Corpus corpus;
}
