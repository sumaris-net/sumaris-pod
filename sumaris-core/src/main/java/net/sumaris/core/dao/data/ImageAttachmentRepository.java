package net.sumaris.core.dao.data;

import net.sumaris.core.dao.technical.jpa.IFetchOptions;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.IDataFilter;

/**
 * @author peck7 on 31/08/2020.
 */
public interface ImageAttachmentRepository extends DataRepository<ImageAttachment, ImageAttachmentVO, IDataFilter, IFetchOptions> {

}
