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
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;

import javax.persistence.*;
import java.util.Date;

/**
 * Liste des rangs taxinomiques possibles.
 *
 *
 *
 * C’est le nom du rang dans la classification systématique ; les niveaux systématiques sont désignés par des termes consacrés (ex. : espèce, genre, famille, etc.). Le niveau systématique d’un taxon peut changer avec l’évolution de la classification ; dans ce cas, son libellé est susceptible de changer également car les suffixes notamment obéissent à des règles strictes de nomenclature (règle [R0018]).
 *
 * Les niveaux systématiques pris en compte dans le référentiel taxinomique Quadrige² sont (par ordre de rang) (le nom anglais de chaque niveau est indiqué entre parenthèses) :
 *
 * - Règne (kingdom)
 *
 * - Sous-règne (subkingdom)
 *
 * - Division (division) / Embranchement (phylum)
 *
 * - Subdivision (subdivision) / Sous-embranchement (subphylum)
 *
 * - Super-classe (superclass)
 *
 * - Classe (class)
 *
 * - Sous-classe (subclass)
 *
 * - Infra-classe (infraclass)
 *
 * - Super-ordre (superordo)
 *
 * - Ordre (ordo)
 *
 * - Sous-ordre (subordo)
 *
 * - Infra-ordre (infraordo)
 *
 * - Section (section)
 *
 * - Sous-section (subsection)
 *
 * - Super-famille (superfamily)
 *
 * - Famille (family)
 *
 * - Sous-famille (subfamily)
 *
 * - Tribu (tribe)
 *
 * - Sous-tribu (subtribe)
 *
 * - Genre (genus)
 *
 * - Sous-genre (subgenus)
 *
 * - Espèce (species)
 *
 * - Sous-espèce (subspecies)
 *
 * - Variété (variety)
 *
 * - Sous-variété (subvariety)
 *
 * - Forme (forma)
 *
 * - Sous-forme (subforma)
 *
 * - Incertae sedis (dummy = taxons inclassables)
 *
 */
@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "taxonomic_level")
public class TaxonomicLevel implements IItemReferentialEntity<Integer> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "TAXONOMIC_LEVEL_SEQ")
    @SequenceGenerator(name = "TAXONOMIC_LEVEL_SEQ", sequenceName="TAXONOMIC_LEVEL_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
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

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;
}
