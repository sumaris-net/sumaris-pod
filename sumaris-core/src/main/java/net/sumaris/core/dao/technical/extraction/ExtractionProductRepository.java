package net.sumaris.core.dao.technical.extraction;

import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ProductFetchOptions;

/**
 * @author peck7 on 21/08/2020.
 */
public interface ExtractionProductRepository
    extends ReferentialRepository<ExtractionProduct, ExtractionProductVO, ExtractionProductFilterVO, ProductFetchOptions>, ExtractionProductRepositoryExtend {

    default ExtractionProductVO getByLabel(String label) {
        return getByLabel(label, null);
    }


}
