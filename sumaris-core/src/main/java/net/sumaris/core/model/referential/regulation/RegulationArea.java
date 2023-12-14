/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.model.referential.regulation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.DepthGradient;
import net.sumaris.core.model.referential.DistanceToCoastGradient;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.NearbySpecificArea;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.taxon.TaxonGroup;

import javax.persistence.*;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "regulation_area")
public class RegulationArea {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REGULATION_AREA_SEQ")
    @SequenceGenerator(name = "REGULATION_AREA_SEQ", sequenceName="REGULATION_AREA_SEQ", allocationSize = IItemReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_fk")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nearby_specific_area_fk")
    private NearbySpecificArea nearbySpecificArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depth_gradient_fk")
    private DepthGradient depthGradient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distance_to_coast_gradient_fk")
    private DistanceToCoastGradient distanceToCoastGradient;

    @ManyToMany
    @JoinTable(
        name = "regulation_area2regulation_location",
        joinColumns = @JoinColumn(name = "regulation_area_fk"),
        inverseJoinColumns = @JoinColumn(name = "location_fk")
    )
    private Set<Location> regulationLocations;

    // Getters and Setters
}
