package net.sumaris.core.model.referential.location;

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
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Cacheable
@Table(name="location_association")
@IdClass(LocationAssociationId.class)
public class LocationAssociation implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_location_fk")
    @EqualsAndHashCode.Include
    private Location parentLocation;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_location_fk")
    @EqualsAndHashCode.Include
    private Location childLocation;

    /**
     * Ratio de couverture (en surface) du lieu fils par rapport au lieu père. La valeur doit etre
     * supérieure strictement à 0 et inférieur ou égale à 1.
     * Un Lieu qui a un ratio de surface de 1 n'a donc qu'un seul lieu père direct. Un lieu qui a un
     * ratio de surface inférieur à 1 peu avoir potentiellement plusieurs lieux pères directs.
     * @return this.childSurfaceRatio Double
     */
    @Column(name = "child_surface_ratio", nullable = false)
    private Double childSurfaceRatio = 1d;

    @Column(name = "is_main_association")
    private Boolean isMainAssociation;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;
}
