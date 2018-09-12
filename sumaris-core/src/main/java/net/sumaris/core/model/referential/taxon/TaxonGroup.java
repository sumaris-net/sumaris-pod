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

import lombok.Data;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

/**
 *  Il désigne un ensemble de taxons appartenant à des groupes taxinomiques différents mais ayant les mêmes caractéristiques pour un critère donné. Ce critère peut être morpho-anatomique (par exemple les strates algales ou la taille des organismes), comportemental (par exemple des groupes trophiques ou des modes de déplacement), ou encore basé sur des notions plus complexes comme la polluo-sensibilité (exemple des groupes écologiques définis pour les macroinvertébrés benthiques). Pour un critère donné, les groupes de taxons sont rassemblés dans un regroupement appelé groupe de taxons père.
 *
 * Les groupes de taxons sont de deux catégories : <ul>
 *   <li>Descriptif : c'est à dire seulement utilisé pour l'extraction de données. Les regroupements de taxons sont effectués en aval de la mesure effectuée.</li>
 *   <li>Identification : il s'agit de regroupements utilisés pour identifier des catégories de taxons sur le terrain ou en laboratoire, lorsqu'il est difficile d'opérer une identification précise ou lorsque celle-ci s'avère tout simplement impossible ou non pertinente. Le regroupement des taxons s'effectue alors en amont de la mesure.</li>
 * </ul>
 *
 * Certains groupes peuvent être figés, c'est à dire qu'ils sont définis une bonne fois pour toute dans un document. Pour ce dernier cas particulier, il n'y a donc, a priori, pas besoin de mise à jour, et encore moins de pouvoir les supprimer : ils sont donc non modifiables (mais ce ne doit pas être une règle générale)
 *
 */
@Data
@Entity
public class TaxonGroup implements IItemReferentialEntity {

    public static final String PROPERTY_TAXON_GROUP_TYPE = "taxonGroupType";

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = TaxonGroupType.class)
    @JoinColumn(name = "taxon_group_type_fk", nullable = false)
    private TaxonGroupType taxonGroupType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = TaxonGroup.class)
    @JoinColumn(name = "parent_taxon_group_fk")
    private TaxonGroup parentTaxonGroup;
}
