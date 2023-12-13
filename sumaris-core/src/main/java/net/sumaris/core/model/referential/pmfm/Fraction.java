package net.sumaris.core.model.referential.pmfm;

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
 * Une fraction analysée est un composant du support sur lequel porte l'analyse.
 *
 *
 *
 * Les fractions analysées sont généralement des fractions "organiques", au sens d'une classification par partie d'un même organisme,
 * Par exemple : foie, écaille, reins, dents, otolithe...
 * Elles peuvent aussi être un sous ensemble quelconque du support. Par exemple, dans le cas des engins : le bras, …
 *
 * Les fractions dites "systématiques", au sens d'une classification systématique (ex : poisson : Cyprinidae / Cyprinus / Cyprinus carpio...) ne sont pas considérées comme des fractions au sens de l'entité, mais comme une précision apportée sur l'individu. Représentées par les entités "taxon" et "groupe de taxon", elles ne font pas partie de la liste des fractions analysées.
 * Etant une liste de référence, une procédure stricte pour la création de nouvelles fractions analysées pourra être mise en place.
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Cacheable
public class Fraction implements IItemReferentialEntity<Integer> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "FRACTION_SEQ")
    @SequenceGenerator(name = "FRACTION_SEQ", sequenceName="FRACTION_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Matrix.class)
    @JoinColumn(name = "matrix_fk")
    private Matrix matrix;
}
