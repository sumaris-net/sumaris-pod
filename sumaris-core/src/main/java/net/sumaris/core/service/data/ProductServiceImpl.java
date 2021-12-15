package net.sumaris.core.service.data;

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


import com.google.common.base.Preconditions;
import net.sumaris.core.dao.data.product.ProductRepository;
import net.sumaris.core.vo.data.ProductVO;
import net.sumaris.core.vo.filter.ProductFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author peck7 on 30/03/2020.
 */
@Service("productService")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductVO get(Integer id) {
        return productRepository.get(id);
    }

    @Override
    public List<ProductVO> getByLandingId(int landingId) {
        return productRepository.findAll(ProductFilterVO.builder().landingId(landingId).build());
    }

    @Override
    public List<ProductVO> getByOperationId(int operationId) {
        return productRepository.findAll(ProductFilterVO.builder().operationId(operationId).build());
    }

    @Override
    public List<ProductVO> getBySaleId(int saleId) {
        return productRepository.findAll(ProductFilterVO.builder().saleId(saleId).build());
    }

    @Override
    public List<ProductVO> getByExpectedSaleId(int expectedSaleId) {
        return productRepository.findAll(ProductFilterVO.builder().expectedSaleId(expectedSaleId).build());
    }

    @Override
    public ProductVO save(ProductVO product) {
        checkProduct(product);

        // Save
        return productRepository.save(product);
    }

    @Override
    public List<ProductVO> save(List<ProductVO> products) {
        Preconditions.checkNotNull(products);
        return products.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> saveByLandingId(int landingId, List<ProductVO> products) {
        checkProducts(products);
        return productRepository.saveByLandingId(landingId, products);
    }

    @Override
    public List<ProductVO> saveByOperationId(int operationId, List<ProductVO> products) {
        checkProducts(products);
        return productRepository.saveByOperationId(operationId, products);
    }

    @Override
    public List<ProductVO> saveBySaleId(int saleId, List<ProductVO> products) {
        checkProducts(products);
        return productRepository.saveBySaleId(saleId, products);
    }

    @Override
    public List<ProductVO> saveByExpectedSaleId(int expectedSaleId, List<ProductVO> products) {
        checkProducts(products);
        return productRepository.saveByExpectedSaleId(expectedSaleId, products);
    }

    @Override
    public void delete(int id) {
        productRepository.deleteById(id);
    }

    @Override
    public void delete(List<Integer> ids) {
        Preconditions.checkNotNull(ids);
        ids.stream()
            .filter(Objects::nonNull)
            .forEach(this::delete);
    }

    @Override
    public ProductVO control(ProductVO product) {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(product.getId());
        Preconditions.checkArgument(product.getControlDate() == null);

        return productRepository.control(product);
    }

    @Override
    public ProductVO validate(ProductVO product) {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(product.getId());
        Preconditions.checkNotNull(product.getControlDate());
        Preconditions.checkArgument(product.getValidationDate() == null);

        return productRepository.validate(product);
    }

    @Override
    public ProductVO unvalidate(ProductVO product) {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(product.getId());
        Preconditions.checkNotNull(product.getControlDate());
        Preconditions.checkNotNull(product.getValidationDate());

        return productRepository.unValidate(product);
    }

    @Override
    public void fillMeasurementsMap(ProductVO product) {
        productRepository.fillMeasurementsMap(product);
    }

    /* protected methods */

    protected void checkProducts(final List<ProductVO> sources) {
        Preconditions.checkNotNull(sources);
        sources.forEach(this::checkProduct);
    }
    protected void checkProduct(final ProductVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
    }
}
