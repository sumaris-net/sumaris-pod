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
 * <p>
 * Les méthodes sont rassemblées dans une liste qui couvre tous les domaines pour lesquels il existe un paramètre.
 * La liste des méthodes est générique et porte sur toutes les phases du processus de mesure des paramètres.
 * Chaque méthode n'est pas non plus systématiquement spécifique à l'une de ces phases ou à une nature particulière
 * de paramètre.
 * En effet, une méthode peut couvrir tout le cycle du processus et/ou être utilisable pour une phase quelle que
 * soit la nature du paramètre.
 * </p>
 *
 * <p>
 * Les méthodes peuvent être référencées par les paramètres à différentes phases de leur processus de mesure que sont :<ul>
 * <li>pour les paramètres biologique :<ul>
 *   <li>le prélèvement et l'échantillonnage ;</li>
 *   <li>la conservation et le transport ;
 *   <li>le fractionnement ;
 *   <li>l'analyse ;
 * </ul></li>
 *
 * <li>pour les paramètres environnementaux :<ul>
 *   <li>l'observation ;</li>
 * </ul>
 * </ul>
 * </p>
 */
@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Cacheable
public class Method implements IItemReferentialEntity<Integer> {

    public static final String ENTITY_NAME = "Method";

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "METHOD_SEQ")
    @SequenceGenerator(name = "METHOD_SEQ", sequenceName="METHOD_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
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

    @Column(name = "is_calculated", nullable = false)
    private Boolean isCalculated;

    @Column(name = "is_estimated", nullable = false)
    private Boolean isEstimated;

    @Column(length = LENGTH_COMMENTS)
    private String comments;
}
