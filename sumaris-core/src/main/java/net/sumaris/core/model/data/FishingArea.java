package net.sumaris.core.model.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.referential.DepthGradient;
import net.sumaris.core.model.referential.DistanceToCoastGradient;
import net.sumaris.core.model.referential.NearbySpecificArea;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;

import javax.persistence.*;
import java.util.Date;


/**
 * @author peck7 on 08/06/2020.
 */
@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "fishing_area")
public class FishingArea implements IEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FISHING_AREA_SEQ")
    @SequenceGenerator(name = "FISHING_AREA_SEQ", sequenceName="FISHING_AREA_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name="qualification_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qualificationDate;

    @Column(name="qualification_comments", length = IDataEntity.LENGTH_COMMENTS)
    private String qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = QualityFlag.class)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    // parent
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Operation.class)
    @JoinColumn(name = "operation_fk")
    @ToString.Exclude
    private Operation operation;

    // TODO : add other parent (from SIH-Adagio model)

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Location.class)
    @JoinColumn(name = "location_fk", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DistanceToCoastGradient.class)
    @JoinColumn(name = "distance_to_coast_gradient_fk")
    private DistanceToCoastGradient distanceToCoastGradient;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DepthGradient.class)
    @JoinColumn(name = "depth_gradient_fk")
    private DepthGradient depthGradient;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = NearbySpecificArea.class)
    @JoinColumn(name = "nearby_specific_area_fk")
    private NearbySpecificArea nearbySpecificArea;

}
