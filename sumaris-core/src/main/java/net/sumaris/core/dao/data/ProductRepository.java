package net.sumaris.core.dao.data;

import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Product;
import net.sumaris.core.vo.data.ProductVO;
import net.sumaris.core.vo.filter.ProductFilterVO;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author peck7 on 30/03/2020.
 */
//@Repository("productRepository")
public interface ProductRepository extends
    DataRepository<Product, Integer, ProductVO, ProductFilterVO>,
    IEntityConverter<Product, ProductVO> {

    default Specification<Product> hasLandingId(Integer landingId) {
        if (landingId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Product.Fields.LANDING).get(IEntity.Fields.ID), landingId);
    }

    default Specification<Product> hasOperationId(Integer operationId) {
        if (operationId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Product.Fields.OPERATION).get(IEntity.Fields.ID), operationId);
    }

    default Specification<Product> hasSaleId(Integer saleId) {
        if (saleId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(Product.Fields.SALE).get(IEntity.Fields.ID), saleId);
    }

    List<ProductVO> saveByOperationId(int operationId, @Nonnull List<ProductVO> products);

    List<ProductVO> saveByLandingId(int landingId, @Nonnull List<ProductVO> products);

    List<ProductVO> saveBySaleId(int saleId, @Nonnull List<ProductVO> products);

}
