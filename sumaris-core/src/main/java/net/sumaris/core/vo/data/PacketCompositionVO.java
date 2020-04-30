package net.sumaris.core.vo.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.List;

/**
 * @author peck7 on 09/04/2020.
 */
@Data
@FieldNameConstants
@EqualsAndHashCode
public class PacketCompositionVO implements IEntity<Integer> {

    @EqualsAndHashCode.Exclude
    private Integer id;
    private Integer rankOrder;
    private ReferentialVO taxonGroup;
    private List<Integer> ratios;

}
