package net.sumaris.core.dao.data.product;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import net.sumaris.core.dao.data.DataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.Product;
import net.sumaris.core.vo.data.ProductVO;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 30/03/2020.
 */
public interface ProductSpecifications extends DataSpecifications<Integer, Product> {

    String LANDING_ID_PARAM = "landingId";
    String OPERATION_ID_PARAM = "operationId";
    String SALE_ID_PARAM = "saleId";
    String EXPECTED_SALE_ID_PARAM = "expectedSaleId";

    default Specification<Product> hasLandingId(Integer landingId) {
        if (landingId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, LANDING_ID_PARAM);
            return cb.equal(root.get(Product.Fields.LANDING).get(IEntity.Fields.ID), param);
        }).addBind(LANDING_ID_PARAM, landingId);
    }

    default Specification<Product> hasOperationId(Integer operationId) {
        if (operationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, OPERATION_ID_PARAM);
            return cb.equal(root.get(Product.Fields.OPERATION).get(IEntity.Fields.ID), param);
        }).addBind(OPERATION_ID_PARAM, operationId);
    }

    default Specification<Product> hasSaleId(Integer saleId) {
        if (saleId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, SALE_ID_PARAM);
            return cb.equal(root.get(Product.Fields.SALE).get(IEntity.Fields.ID), param);
        }).addBind(SALE_ID_PARAM, saleId);
    }

    default Specification<Product> hasExpectedSaleId(Integer expectedSaleId) {
        if (expectedSaleId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, EXPECTED_SALE_ID_PARAM);
            return cb.equal(root.get(Product.Fields.EXPECTED_SALE).get(IEntity.Fields.ID), param);
        }).addBind(EXPECTED_SALE_ID_PARAM, expectedSaleId);
    }

    List<ProductVO> saveByOperationId(int operationId, @Nonnull List<ProductVO> products);

    List<ProductVO> saveByLandingId(int landingId, @Nonnull List<ProductVO> products);

    List<ProductVO> saveBySaleId(int saleId, @Nonnull List<ProductVO> products);

    List<ProductVO> saveByExpectedSaleId(int expectedSaleId, @Nonnull List<ProductVO> products);

    void fillMeasurementsMap(ProductVO product);
}
