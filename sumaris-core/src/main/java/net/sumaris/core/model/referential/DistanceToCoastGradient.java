package net.sumaris.core.model.referential;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.util.Date;

/**
 * @author peck7 on 08/06/2020.
 */
@Data
@FieldNameConstants
@Entity
@Table(name = "distance_to_coast_gradient")
public class DistanceToCoastGradient implements IItemReferentialEntity, IWithDescriptionAndCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "distance_to_coast_gradient_seq")
    @SequenceGenerator(name = "distance_to_coast_gradient_seq", sequenceName="distance_to_coast_gradient_seq", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;
}
