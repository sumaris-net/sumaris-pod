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

import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IWithValidityStatusEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.ValidityStatus;

import javax.persistence.*;
import java.util.Date;

/**
 * Un regroupement représente un groupement d'entités (du référentiel ou bien de navires),
 * correspondant à un niveau de regroupement donné (GroupingLevel).
 *
 * Un regroupement peut faire référence à une ou plusieurs entités du référentiel (GroupingItem).
 * Généralement, surtout les regroupements de plus bas niveau sont liés à des entités du référentiel
 * (plus facile à maintenir), puis une table technique remplie toutes les associations possibles avec les ancêtres
 * (cf GroupingItemHierarchy).
 *
 * Un regroupement peut avoir un regroupement parent.
 */
@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
// FIXME: rename with quote ? BUT query on this table will failed ! (e.g. lastUpdateDate)
@Table(name = "grouping",
       uniqueConstraints = @UniqueConstraint(name="grouping_unique_c", columnNames = {"label", "grouping_level_fk"}))
public class Grouping implements IItemReferentialEntity<Integer>,
        IWithValidityStatusEntity<Integer, ValidityStatus> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROUPING_SEQ")
    @SequenceGenerator(name = "GROUPING_SEQ", sequenceName="GROUPING_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @ToString.Include
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

    /**
     * Valeur minimum de la caractéristique de regroupement.
     * Uniquement dans le cas où une caractéristique a été définie sur le niveau de regroupement parent
     * (uniquement si GroupLevel.featuresPmfm est renseigné).
     *
     * Exemple : Si la caractéristique est "Longueur du navire", la valeur min vaudra "10", pour une classe de taille de 10 à 20 m.
     */
    @Column(name = "min_value")
    private Double minValue;

    /**
     * Valeur maximale de la caractéristique de regroupement.
     * Uniquement dans le cas où une caractéristique a été définie sur le niveau de regroupement parent
     * (uniquement si GroupLevel.featuresPmfm est renseigné).
     * Par convention, la valeur maximale du regroupement est EXCLUE.
     *
     * Exemple : Si la caractéristique est "Longueur du navire", la valeur max vaudra "20", pour une classe de taille
     * de [10-20[m (20 étant exclu).
     */
    @Column(name = "max_value")
    private Double maxValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validity_status_fk", nullable = false)
    private ValidityStatus validityStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grouping_classification_fk", nullable = false)
    private GroupingClassification groupingClassification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grouping_level_fk", nullable = false)
    private GroupingLevel groupingLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_grouping_fk")
    private Grouping parentGrouping;

}
