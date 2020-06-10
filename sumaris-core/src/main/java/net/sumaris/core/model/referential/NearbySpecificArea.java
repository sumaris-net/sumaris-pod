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
@Table(name = "nearby_specific_area")
public class NearbySpecificArea implements IItemReferentialEntity, IWithDescriptionAndCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nearby_specific_area_seq")
    @SequenceGenerator(name = "nearby_specific_area_seq", sequenceName="nearby_specific_area_seq", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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

}
