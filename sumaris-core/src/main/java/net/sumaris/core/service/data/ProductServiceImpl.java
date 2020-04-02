package net.sumaris.core.service.data;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.data.ProductRepository;
import net.sumaris.core.service.referential.PmfmService;
import net.sumaris.core.vo.data.ProductVO;
import net.sumaris.core.vo.filter.ProductFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author peck7 on 30/03/2020.
 */
@Service("productService")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final PmfmService pmfmService;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, PmfmService pmfmService) {
        this.productRepository = productRepository;
        this.pmfmService = pmfmService;
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
    public ProductVO save(ProductVO product) {
        Preconditions.checkNotNull(product);
        Preconditions.checkNotNull(product.getRecorderDepartment(), "Missing recorderDepartment");
        Preconditions.checkNotNull(product.getRecorderDepartment().getId(), "Missing recorderDepartment.id");

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

        // Copy well-known measurements to product attributes
//        copyMeasurements(products);

        // Save entities
        List<ProductVO> result = productRepository.saveByLandingId(landingId, products);

        // Save measurements
        saveMeasurements(result);

        return result;
    }

    @Override
    public List<ProductVO> saveByOperationId(int operationId, List<ProductVO> products) {

        // Save entities
        List<ProductVO> result = productRepository.saveByOperationId(operationId, products);

        // Save measurements
        saveMeasurements(result);

        return result;

    }

    @Override
    public List<ProductVO> saveBySaleId(int saleId, List<ProductVO> products) {

        // Save entities
        List<ProductVO> result = productRepository.saveBySaleId(saleId, products);

        // Save measurements
        saveMeasurements(result);

        return result;

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

        return productRepository.unvalidate(product);
    }


    private void copyMeasurements(List<ProductVO> products) {

        if (products == null) return;

        products.forEach(this::copyMeasurements);

    }

    private void copyMeasurements(ProductVO product) {

        if (product == null) return;

        if (product.getMeasurementValues() != null) {

            Set<Integer> pmfmToRemove = new HashSet<>();

            product.getMeasurementValues().forEach((pmfmId, value) -> {
                if (pmfmService.isWeightPmfm(pmfmId)) {
                    pmfmToRemove.add(pmfmId);
//                    product.setWeight(value);
                    if (pmfmService.isCalculatedPmfm(pmfmId)) {

                    }
                }
            });

        }

    }


    private void saveMeasurements(List<ProductVO> result) {



    }


}
