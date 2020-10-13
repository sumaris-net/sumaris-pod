package net.sumaris.core.dao.technical.extraction;

import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;

/**
 * Give access to extraction products
 */
public interface ExtractionProductRepository
    extends ReferentialRepository<ExtractionProduct, ExtractionProductVO, ExtractionProductFilterVO, ExtractionProductFetchOptions>, ExtractionProductSpecifications {

    default ExtractionProductVO getByLabel(String label) {
        return getByLabel(label, null);
    }


}
