package net.sumaris.core.model.referential.grouping;

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
 * Niveau de regroupement, d'une classification donnée. *
 * <p>
 *  Exemple : pour les métiers, on peut avoir les niveaux de regroupement suivants : arts, grandes familles d’engins, …
 * </p>
 *
 * <ul>
 *     <li>Un niveau de regroupement peut avoir un niveau parent.</li>
 *     <li>Un niveau de regroupement contient un ou plusieurs regroupements (Grouping).</li>
 * </ul>
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "grouping_level")
public class GroupingLevel implements IItemReferentialEntity<Integer>  {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROUPING_LEVEL_SEQ")
    @SequenceGenerator(name = "GROUPING_LEVEL_SEQ", sequenceName="GROUPING_LEVEL_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @ToString.Include
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
    @ToString.Include
    private String label;

    @Column(nullable = false, length = LENGTH_NAME)
    private String name;

    private String description;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "grouping_classification_fk")
    private GroupingClassification groupingClassification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_grouping_level_fk")
    private GroupingLevel parentGroupingLevel;

    // Pmfm ? To remove -> never used in Harmonie
}
