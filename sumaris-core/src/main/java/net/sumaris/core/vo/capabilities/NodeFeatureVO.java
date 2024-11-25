package net.sumaris.core.vo.capabilities;

import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class NodeFeatureVO {
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;
    @ToString.Include
    private String label;
    @ToString.Include
    private String name;
    private String description;

    private String logo;

    private Date updateDate;
    private Date creationDate;

    private Integer statusId;
}
