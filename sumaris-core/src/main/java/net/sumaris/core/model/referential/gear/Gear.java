package net.sumaris.core.model.referential.gear;

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
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "gear")
@Cacheable
public class Gear implements IItemReferentialEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GEAR_SEQ")
    @SequenceGenerator(name = "GEAR_SEQ", sequenceName="GEAR_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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
     * Indique si l'engin est actif ou passif :<ul>
     *     <li>Actif (=vrai) : l'engin se déplace pour capturer l'espèce recherchée.</li>
     *     <li>Passif (=faux) : l'engin attend que la cible se prenne dedans.</li>
     * </ul>
     */
    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * Indique si le type d'engin est trainant ou dormant.<br/>
     * Actuellement, isTowed (à défaut d'autre champ libre) a été rempli pour  permettre son utilisation dans Allegro avec le sens suivant : « autorise ou non la  superposition d'opérations de pêche simultanées » :
     * <ul>
     *     <li>is_towed=1 : l'opération utilisant cet engin ne peut pas être superposée avec d'autres opérations ayant aussi un engin is_towed=1</li>
     *     <li>is_towed=0 : superposition avec d'autres opérations possibles (quelques soit la valeur is_towed de leur engin)</li>
     * </ul>
     */
    @Column(name = "is_towed")
    private Boolean isTowed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gear_classification_fk", nullable = false)
    private GearClassification gearClassification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_gear_fk")
    private Gear parent;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Gear.class, mappedBy = Fields.PARENT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Gear> children;

    @ManyToMany(fetch = FetchType.LAZY, targetEntity = Strategy.class, mappedBy = Strategy.Fields.GEARS)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    private List<Strategy> strategies;

}
