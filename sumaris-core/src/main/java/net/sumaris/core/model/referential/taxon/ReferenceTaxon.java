package net.sumaris.core.model.referential.taxon;

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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.ModelVocabularies;
import net.sumaris.core.model.administration.programStrategy.ReferenceTaxonStrategy;
import net.sumaris.core.model.annotation.OntologyEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.technical.optimization.taxon.TaxonGroup2TaxonHierarchy;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Un référence stable (un identifiant unique) de taxon réel.
 *
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "reference_taxon")
@OntologyEntity(vocab = ModelVocabularies.TAXON)
public class ReferenceTaxon implements IUpdateDateEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REFERENCE_TAXON_SEQ")
    @SequenceGenerator(name = "REFERENCE_TAXON_SEQ", sequenceName="REFERENCE_TAXON_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;


    @OneToMany(fetch = FetchType.LAZY, targetEntity = TaxonName.class, mappedBy = TaxonName.Fields.REFERENCE_TAXON)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<TaxonName> taxonNames;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ReferenceTaxonStrategy.class, mappedBy = ReferenceTaxonStrategy.Fields.REFERENCE_TAXON)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    private List<ReferenceTaxonStrategy> strategies;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = TaxonGroup2TaxonHierarchy.class, mappedBy = TaxonGroup2TaxonHierarchy.Fields.CHILD_REFERENCE_TAXON)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    private List<TaxonGroup2TaxonHierarchy> parentTaxonGroups;

}
