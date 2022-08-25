package net.sumaris.core.model.administration.programStrategy;

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

@Data
@FieldNameConstants
@Entity
@Table(name = "taxon_group_strategy")
public class TaxonGroupStrategy implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Strategy.class)
    @JoinColumn(name = "strategy_fk")
    private Strategy strategy;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = TaxonGroup.class)
    @JoinColumn(name = "taxon_group_fk")
    private TaxonGroup taxonGroup;

    /**
     * Niveau de priorité de collecte de données sur le groupe de taxon.
     * Si non renseigné, il faut alors considérer qu'aucun niveau de priorité n'est utilisé dans la stratégie.
     *
     * Attention, il ne s'agit pas d'un rankOrder ! Ici, il peut y avoir plusieurs groupe de taxon avec le même priorityLevel.
     */
    @Column(name = "priority_level")
    private Integer priorityLevel;



}
