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
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Product;
import net.sumaris.core.vo.data.ProductVO;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 30/03/2020.
 */
public interface ProductRepositoryExtend extends DataSpecifications<Product> {

    String LANDING_ID_PARAM = "landingId";
    String OPERATION_ID_PARAM = "operationId";
    String SALE_ID_PARAM = "saleId";

    default Specification<Product> hasLandingId(Integer landingId) {
        BindableSpecification<Product> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, LANDING_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Product.Fields.LANDING).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(LANDING_ID_PARAM, landingId);
        return specification;
    }

    default Specification<Product> hasOperationId(Integer operationId) {
        BindableSpecification<Product> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, OPERATION_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Product.Fields.OPERATION).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(OPERATION_ID_PARAM, operationId);
        return specification;
    }

    default Specification<Product> hasSaleId(Integer saleId) {
        BindableSpecification<Product> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, SALE_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Product.Fields.SALE).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(SALE_ID_PARAM, saleId);
        return specification;
    }

    List<ProductVO> saveByOperationId(int operationId, @Nonnull List<ProductVO> products);

    List<ProductVO> saveByLandingId(int landingId, @Nonnull List<ProductVO> products);

    List<ProductVO> saveBySaleId(int saleId, @Nonnull List<ProductVO> products);

    void fillMeasurementsMap(ProductVO product);
}
