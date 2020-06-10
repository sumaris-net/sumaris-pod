package net.sumaris.core.model.data;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IEntity;
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
@Data
@FieldNameConstants
@Entity
@Table(name = "fishing_area")
public class FishingArea implements IEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FISHING_AREA_SEQ")
    @SequenceGenerator(name = "FISHING_AREA_SEQ", sequenceName="FISHING_AREA_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
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
    @JoinColumn(name = "operation_fk", nullable = false)
    private Operation operation;

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
