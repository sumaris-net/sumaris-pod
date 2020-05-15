package net.sumaris.core.dao.data;

import net.sumaris.core.model.data.Product;
import net.sumaris.core.vo.data.ProductVO;
import net.sumaris.core.vo.filter.ProductFilterVO;

import java.util.Collection;

/**
 * @author peck7 on 30/03/2020.
 */
public interface ProductRepository extends
    DataRepository<Product, Integer, ProductVO, ProductFilterVO>,
    ProductRepositoryExtend {

    void deleteProductsByBatchIdIn(Collection<Integer> batchIds);
}
