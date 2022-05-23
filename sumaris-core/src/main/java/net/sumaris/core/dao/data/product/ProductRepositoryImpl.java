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

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.data.DataDaos;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.pmfm.Method;
import net.sumaris.core.model.referential.pmfm.MethodEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ProductVO;
import net.sumaris.core.vo.filter.ProductFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 30/03/2020.
 */
@Slf4j
public class ProductRepositoryImpl
    extends DataRepositoryImpl<Product, ProductVO, ProductFilterVO, DataFetchOptions>
    implements ProductSpecifications {

    private final ReferentialDao referentialDao;
    private final PersonRepository personRepository;
    private final LandingRepository landingRepository;
    private final PmfmRepository pmfmRepository;
    private final MeasurementDao measurementDao;

    private final Map<String, Integer> pmfmIdByLabel;

    @Autowired
    public ProductRepositoryImpl(EntityManager entityManager,
                                 ReferentialDao referentialDao,
                                 PersonRepository personRepository,
                                 PmfmRepository pmfmRepository,
                                 LandingRepository landingRepository,
                                 MeasurementDao measurementDao) {
        super(Product.class, ProductVO.class, entityManager);
        this.referentialDao = referentialDao;
        this.personRepository = personRepository;
        this.landingRepository = landingRepository;
        this.pmfmRepository = pmfmRepository;
        this.measurementDao = measurementDao;

        setCheckUpdateDate(false);

        pmfmIdByLabel = new HashMap<>();
    }

    @Override
    public Specification<Product> toSpecification(ProductFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(hasLandingId(filter.getLandingId()))
            .and(hasOperationId(filter.getOperationId()))
            .and(hasSaleId(filter.getSaleId()))
            .and(hasExpectedSaleId(filter.getExpectedSaleId()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()));
    }

    @Override
    public void toVO(Product source, ProductVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Recorder person
        if ((fetchOptions == null || fetchOptions.isWithRecorderPerson()) && source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personRepository.toVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
        }

        if (source.getTaxonGroup() != null) {
            target.setTaxonGroup(referentialDao.toVO(source.getTaxonGroup()));
        }
        if (source.getSaleType() != null) {
            target.setSaleType(referentialDao.toVO(source.getSaleType()));
        }
        // Weight and weight method
        target.setWeight(Daos.roundValue(source.getWeight()));
        if (source.getWeightMethod() != null) {
            target.setWeightCalculated(source.getWeightMethod().getId().equals(MethodEnum.CALCULATED.getId()));
        }

        target.setDressingId(Optional.ofNullable(source.getDressing()).map(QualitativeValue::getId).orElse(null));
        target.setPreservationId(Optional.ofNullable(source.getPreservation()).map(QualitativeValue::getId).orElse(null));
        target.setSizeCategoryId(Optional.ofNullable(source.getSizeCategory()).map(QualitativeValue::getId).orElse(null));

        // Parent link
        if (source.getLanding() != null) {
            target.setLandingId(source.getLanding().getId());
        }
        if (source.getOperation() != null) {
            target.setOperationId(source.getOperation().getId());
        }
        if (source.getSale() != null) {
            target.setSaleId(source.getSale().getId());
        }
        if (source.getExpectedSale() != null) {
            target.setExpectedSaleId(source.getExpectedSale().getId());
        }
        if (source.getBatch() != null) {
            target.setBatchId(source.getBatch().getId());
        }
    }

    @Override
    public void toEntity(ProductVO source, Product target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Recorder person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);

        // Taxon group
        if (copyIfNull || source.getTaxonGroup() != null) {
            if (source.getTaxonGroup() == null || source.getTaxonGroup().getId() == null) {
                target.setTaxonGroup(null);
            } else {
                target.setTaxonGroup(getReference(TaxonGroup.class, source.getTaxonGroup().getId()));
            }
        }

        // Sale type
        if (copyIfNull || source.getSaleType() != null) {
            if (source.getSaleType() == null || source.getSaleType().getId() == null) {
                target.setSaleType(null);
            } else {
                target.setSaleType(getReference(SaleType.class, source.getSaleType().getId()));
            }
        }

        // Weight method if weight exists
        if (source.getWeight() != null) {
            if (source.isWeightCalculated()) {
                target.setWeightMethod(getReference(Method.class, MethodEnum.CALCULATED.getId()));
            } else {
                target.setWeightMethod(getReference(Method.class, MethodEnum.MEASURED_BY_OBSERVER.getId()));
            }
        } else {
            target.setWeightMethod(null);
        }

        // Dressing
        QualitativeValue dressing = extractMeasurementQualitativeValue(source, PmfmEnum.DRESSING.getId());
        if (copyIfNull || dressing != null) {
            target.setDressing(dressing);
        }

        // Preservation
        QualitativeValue preservation = extractMeasurementQualitativeValue(source, PmfmEnum.PRESERVATION.getId());
        if (copyIfNull || preservation != null) {
            target.setPreservation(preservation);
        }

        // Size Category
        QualitativeValue sizeCategory = extractMeasurementQualitativeValue(source, PmfmEnum.SIZE_CATEGORY.getId());
        if (copyIfNull || sizeCategory != null) {
            target.setSizeCategory(sizeCategory);
        }

        // Cost
        Double cost = extractMeasurementNumericalValue(source, PmfmEnum.TOTAL_PRICE.getId());
        if (copyIfNull || cost != null) {
            target.setCost(cost);
        }

        // Landing
        Integer landingId = source.getLandingId() != null ? source.getLandingId() : (source.getLanding() != null ? source.getLanding().getId() : null);
        if (copyIfNull || (landingId != null)) {
            target.setLanding(landingId == null ? null : getReference(Landing.class, landingId));
        }

        // Operation
        Integer operationId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
        if (copyIfNull || (operationId != null)) {
            target.setOperation(operationId == null ? null : getReference(Operation.class, operationId));
        }

        // Sale
        Integer saleId = source.getSaleId() != null ? source.getSaleId() : (source.getSale() != null ? source.getSale().getId() : null);
        if (copyIfNull || (saleId != null)) {
            target.setSale(saleId == null ? null : getReference(Sale.class, saleId));
        }

        // ExpectedSale
        Integer expectedSaleId = source.getExpectedSaleId() != null ? source.getExpectedSaleId() : (source.getExpectedSale() != null ? source.getExpectedSale().getId() : null);
        if (copyIfNull || (expectedSaleId != null)) {
            target.setExpectedSale(expectedSaleId == null ? null : getReference(ExpectedSale.class, expectedSaleId));
        }

        // Batch (link for sale on batch)
        Integer batchId = source.getBatchId() != null ? source.getBatchId() : (source.getBatch() != null ? source.getBatch().getId() : null);
        if (copyIfNull || (batchId != null)) {
            target.setBatch(batchId == null ? null : getReference(Batch.class, batchId));
        }

    }

    @Override
    public List<ProductVO> saveByOperationId(int operationId, @Nonnull List<ProductVO> products) {

        // Load parent entity
        Operation parent = getById(Operation.class, operationId);

        products.forEach(source -> {
            source.setOperationId(operationId);
            source.setLandingId(null);
            source.setLanding(null);
            source.setSaleId(null);
            source.setSale(null);
            source.setExpectedSaleId(null);
            source.setExpectedSale(null);
            // set default weight method
            source.setWeightCalculated(false);
        });

        // Save all by parent
        return saveByParent(parent, products);
    }

    @Override
    public List<ProductVO> saveByLandingId(int landingId, @Nonnull List<ProductVO> products) {

        // Load parent entity
        Landing parent = getById(Landing.class, landingId);

        products.forEach(source -> {
            source.setLandingId(landingId);
            source.setOperationId(null);
            source.setOperation(null);
            source.setSaleId(null);
            source.setSale(null);
            source.setExpectedSaleId(null);
            source.setExpectedSale(null);
        });

        // Save all by parent
        return saveByParent(parent, products);
    }

    @Override
    public List<ProductVO> saveBySaleId(int saleId, @Nonnull List<ProductVO> products) {

        // Load parent entity
        Sale parent = getById(Sale.class, saleId);

        // Get landing Id (to optimize linked data for SIH)
        Integer landingId = Optional.ofNullable(parent.getTrip())
            .flatMap(trip -> landingRepository.findFirstByTripId(trip.getId()))
            .map(Landing::getId)
            .orElse(null);

        products.forEach(source -> {
            source.setSaleId(saleId);
            source.setLandingId(landingId);
            source.setExpectedSaleId(null);
            source.setExpectedSale(null);
            source.setOperationId(null);
            source.setOperation(null);
        });

        // Save all by parent
        return saveByParent(parent, products);
    }

    @Override
    public List<ProductVO> saveByExpectedSaleId(int expectedSaleId, @Nonnull List<ProductVO> products) {
        // Load parent entity
        ExpectedSale parent = getById(ExpectedSale.class, expectedSaleId);

        // Get landing Id (to optimize linked data for SIH)
        Integer landingId = Optional.ofNullable(parent.getTrip())
            .flatMap(trip -> landingRepository.findFirstByTripId(trip.getId()))
            .map(Landing::getId)
            .orElse(null);

        products.forEach(source -> {
            source.setExpectedSaleId(expectedSaleId);
            source.setLandingId(landingId);
            source.setSaleId(null);
            source.setSale(null);
            source.setOperationId(null);
            source.setOperation(null);
        });

        // Save all by parent
        return saveByParent(parent, products);
    }

    @Override
    public void fillMeasurementsMap(ProductVO product) {

        if (product.getMeasurementValues() == null)
            product.setMeasurementValues(new HashMap<>());

        if (product.getDressingId() != null) {
            product.getMeasurementValues().put(PmfmEnum.DRESSING.getId(), product.getDressingId().toString());
        }
        if (product.getPreservationId() != null) {
            product.getMeasurementValues().put(PmfmEnum.PRESERVATION.getId(), product.getPreservationId().toString());
        }
        if (product.getSizeCategoryId() != null) {
            product.getMeasurementValues().put(PmfmEnum.SIZE_CATEGORY.getId(), product.getSizeCategoryId().toString());
        }
        if (product.getCost() != null) {
            product.getMeasurementValues().put(PmfmEnum.TOTAL_PRICE.getId(), product.getCost().toString());
        }

        product.getMeasurementValues().putAll(measurementDao.getProductSortingMeasurementsMap(product.getId()));
        product.getMeasurementValues().putAll(measurementDao.getProductQuantificationMeasurementsMap(product.getId()));

        // Sale specific :
        if (product.getSaleId() != null || product.getExpectedSaleId() != null) {
            // Get average price per packaging and set it to generic average price pmfm
            String packagingId = product.getMeasurementValues().get(PmfmEnum.PACKAGING.getId());
            if (StringUtils.isNotBlank(packagingId)) {
                QualitativeValue packaging = getReference(QualitativeValue.class, Integer.valueOf(packagingId));
                Integer pmfmId = getAveragePricePmfmIdByPackaging(packaging);
                // AVERAGE_WEIGHT_PRICE must be used as is, don't convert to packaging-specific pmfm
                if (!Objects.equals(pmfmId, PmfmEnum.AVERAGE_WEIGHT_PRICE.getId())) {
                    String avgPackagingPrice = product.getMeasurementValues().remove(pmfmId);
                    if (StringUtils.isNotBlank(avgPackagingPrice)) {
                        // put it as generic avg price
                        product.getMeasurementValues().put(PmfmEnum.AVERAGE_PACKAGING_PRICE.getId(), avgPackagingPrice);
                    }
                }
            }

            // Convert sale ratio as percentage
            String ratioText = product.getMeasurementValues().get(PmfmEnum.SALE_ESTIMATED_RATIO.getId());
            if (StringUtils.isNotBlank(ratioText)) {
                product.getMeasurementValues().put(PmfmEnum.SALE_ESTIMATED_RATIO.getId(), Double.toString((Double.parseDouble(ratioText) * 100)));
            }

        }
    }

    protected List<ProductVO> saveByParent(@Nonnull IWithProductsEntity<Integer, Product> parent, @Nonnull List<ProductVO> products) {

        // Load existing entities
        Set<Integer> existingProductIds = parent.getProducts().stream().map(Product::getId).collect(Collectors.toSet());

        products.forEach(product -> {
            save(product);
            existingProductIds.remove(product.getId());
        });

        saveMeasurements(products);

        // Delete remaining
        existingProductIds.forEach(this::deleteById);

        return products;
    }

    private void saveMeasurements(List<ProductVO> products) {

        products.forEach(product -> {

            if (product.getMeasurementValues() != null) {

                // Sale specific :
                if (product.getSaleId() != null || product.getExpectedSaleId() != null) {

                    // Replace average price per packaging by packaging depending average price
                    String packagingId = product.getMeasurementValues().get(PmfmEnum.PACKAGING.getId());
                    String averagePrice = product.getMeasurementValues().get(PmfmEnum.AVERAGE_PACKAGING_PRICE.getId());
                    if (StringUtils.isNotBlank(packagingId) && StringUtils.isNotBlank(averagePrice)) {
                        QualitativeValue packaging = getReference(QualitativeValue.class, Integer.valueOf(packagingId));
                        Integer averagePriceForPackingPmfmId = getAveragePricePmfmIdByPackaging(packaging);
                        if (averagePriceForPackingPmfmId != null) {
                            // put it as specified packaging avg price
                            product.getMeasurementValues().put(averagePriceForPackingPmfmId, product.getMeasurementValues().remove(PmfmEnum.AVERAGE_PACKAGING_PRICE.getId()));
                        }
                    }

                    // Sale ratio must be converted from percentage
                    String ratioText = product.getMeasurementValues().get(PmfmEnum.SALE_ESTIMATED_RATIO.getId());
                    if (StringUtils.isNotBlank(ratioText)) {
                        product.getMeasurementValues().put(PmfmEnum.SALE_ESTIMATED_RATIO.getId(), Double.toString((Double.parseDouble(ratioText) / 100)));
                    }

                    // If rank order is present on a batch (=packet) produce sale, add rank order as sale rank order measurement
                    if (product.getRankOrder() != null && product.getBatchId() != null) {
                        product.getMeasurementValues().put(PmfmEnum.SALE_RANK_ORDER.getId(), product.getRankOrder().toString());
                    }

                }

                Map<Integer, String> quantificationMeasurements = Maps.newLinkedHashMap();
                Map<Integer, String> sortingMeasurements = Maps.newLinkedHashMap();
                product.getMeasurementValues().forEach((pmfmId, value) -> {
                    if (isWeightPmfm(pmfmId)) {
                        quantificationMeasurements.putIfAbsent(pmfmId, value);
                    } else {
                        if (sortingMeasurements.containsKey(pmfmId)) {
                            log.warn(String.format("Duplicate measurement width {pmfmId: %s} on product {id: %s}", pmfmId, product.getId()));
                        } else {
                            sortingMeasurements.putIfAbsent(pmfmId, value);
                        }
                    }
                });
                measurementDao.saveProductSortingMeasurementsMap(product.getId(), sortingMeasurements);
                measurementDao.saveProductQuantificationMeasurementsMap(product.getId(), quantificationMeasurements);
            }
        });

    }

    private QualitativeValue extractMeasurementQualitativeValue(ProductVO source, Integer pmfmId) {

        QualitativeValue result = null;

        if (MapUtils.isNotEmpty(source.getMeasurementValues()) && pmfmId != null) {

            String value = source.getMeasurementValues().remove(pmfmId);

            if (value != null) {
                result = getReference(QualitativeValue.class, Integer.parseInt(value));
            }
        }

        return result;
    }

    private Double extractMeasurementNumericalValue(ProductVO source, Integer pmfmId) {
        Double result = null;

        if (MapUtils.isNotEmpty(source.getMeasurementValues()) && pmfmId != null) {

            String value = source.getMeasurementValues().remove(pmfmId);

            if (value != null) {
                result = Double.parseDouble(value);
            }
        }

        return result;
    }

    private boolean isWeightPmfm(int pmfmId) {
        return pmfmRepository.hasLabelSuffix(pmfmId, "WEIGHT");
    }

    private Integer getAveragePricePmfmIdByPackaging(QualitativeValue packaging) {
        String pmfmLabel = "AVERAGE_PRICE_" + packaging.getLabel();
        pmfmIdByLabel.putIfAbsent(pmfmLabel,
            pmfmRepository.findByLabel(pmfmLabel)
                .map(PmfmVO::getId)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Unable to find pmfm with label '%s'", pmfmLabel))));
        return pmfmIdByLabel.get(pmfmLabel);
    }

}
