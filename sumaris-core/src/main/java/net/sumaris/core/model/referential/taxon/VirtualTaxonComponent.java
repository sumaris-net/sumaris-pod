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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.ModelVocabularies;
import net.sumaris.core.model.annotation.OntologyEntity;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Définit les ReferenceTaxon constituants un TaxonName virtuel
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "virtual_component")
@OntologyEntity(vocab = ModelVocabularies.TAXON)
public class VirtualTaxonComponent implements Serializable  {

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxon_name_fk", nullable = false)
    @EqualsAndHashCode.Include
    private TaxonName taxonName;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reference_taxon_fk", nullable = false)
    @EqualsAndHashCode.Include
    private ReferenceTaxon referenceTaxon;

}
