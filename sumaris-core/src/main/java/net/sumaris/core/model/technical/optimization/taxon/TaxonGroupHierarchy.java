package net.sumaris.core.model.technical.optimization.taxon;

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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.taxon.TaxonGroup;

import javax.persistence.*;
import java.io.Serializable;

/**
 *  Table technique présentant tous les liens père/fils (directs et hérités) entre les groupes de taxons.
 *
 *  Des liens entre chaque groupe de taxon et lui même y sont également présents, pour faciliter l'utilisation de cette table.
 *
 *  Cette table est remplie à partir du contenu de TaxonGroup, par un procédure stockée à lancer depuis la base de données.
 *
 */
@Data
@FieldNameConstants
@Entity
@Table(name = "taxon_group_hierarchy")
public class TaxonGroupHierarchy implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = TaxonGroup.class, cascade = CascadeType.DETACH)
    @JoinColumn(name = "parent_taxon_group_fk", nullable = false)
    private TaxonGroup parentTaxonGroup;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = TaxonGroup.class, cascade = CascadeType.DETACH)
    @JoinColumn(name = "child_taxon_group_fk", nullable = false)
    private TaxonGroup childTaxonGroup;
}
