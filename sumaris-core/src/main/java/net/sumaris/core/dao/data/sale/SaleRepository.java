package net.sumaris.core.dao.data.sale;

import net.sumaris.core.dao.data.RootDataRepository;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.SaleVO;
import net.sumaris.core.vo.filter.SaleFilterVO;

/**
 * @author peck7 on 01/09/2020.
 */
public interface SaleRepository
    extends RootDataRepository<Sale, SaleVO, SaleFilterVO, DataFetchOptions>, SaleSpecifications {

}
