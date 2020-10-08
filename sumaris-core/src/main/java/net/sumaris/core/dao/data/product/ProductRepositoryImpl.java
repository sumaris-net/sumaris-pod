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
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.data.DataDaos;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.technical.Daos;
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
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.filter.ProductFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 30/03/2020.
 */
public class ProductRepositoryImpl
    extends DataRepositoryImpl<Product, ProductVO, ProductFilterVO, DataFetchOptions>
    implements ProductSpecifications {

    private static final Logger LOG = LoggerFactory.getLogger(ProductRepositoryImpl.class);

    private final ReferentialDao referentialDao;
    private final PersonRepository personRepository;
    private final LandingRepository landingRepository;
    private final PmfmRepository pmfmRepository;
    private final MeasurementDao measurementDao;

    private final Map<String, Integer> pmfmIdByLabel;
    private Integer measuredMethodId;
    private Integer calculatedMethodId;

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
    public Specification<Product> toSpecification(ProductFilterVO filter) {
        return super.toSpecification(filter)
            .and(hasLandingId(filter.getLandingId()))
            .and(hasOperationId(filter.getOperationId()))
            .and(hasSaleId(filter.getSaleId()));
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
            target.setWeightCalculated(source.getWeightMethod().getId().equals(getCalculatedMethodId()));
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
                target.setTaxonGroup(load(TaxonGroup.class, source.getTaxonGroup().getId()));
            }
        }

        // Sale type
        if (copyIfNull || source.getSaleType() != null) {
            if (source.getSaleType() == null || source.getSaleType().getId() == null) {
                target.setSaleType(null);
            } else {
                target.setSaleType(load(SaleType.class, source.getSaleType().getId()));
            }
        }

        // Weight method if weight exists
        if (source.getWeight() != null) {
            if (source.isWeightCalculated()) {
                target.setWeightMethod(load(Method.class, getCalculatedMethodId()));
            } else {
                target.setWeightMethod(load(Method.class, getMeasuredMethodId()));
            }
        } else {
            target.setWeightMethod(null);
        }

        // Dressing
        QualitativeValue dressing = extractMeasurementQualitativeValue(source, getPmfmIdByPmfmEnum(PmfmEnum.DRESSING));
        if (copyIfNull || dressing != null) {
            target.setDressing(dressing);
        }

        // Preservation
        QualitativeValue preservation = extractMeasurementQualitativeValue(source, getPmfmIdByPmfmEnum(PmfmEnum.PRESERVATION));
        if (copyIfNull || preservation != null) {
            target.setPreservation(preservation);
        }

        // Size Category
        QualitativeValue sizeCategory = extractMeasurementQualitativeValue(source, getPmfmIdByPmfmEnum(PmfmEnum.SIZE_CATEGORY));
        if (copyIfNull || sizeCategory != null) {
            target.setSizeCategory(sizeCategory);
        }

        // Cost
        Double cost = extractMeasurementNumericalValue(source, getPmfmIdByPmfmEnum(PmfmEnum.TOTAL_PRICE));
        if (copyIfNull || cost != null) {
            target.setCost(cost);
        }

        // Landing
        Integer landingId = source.getLandingId() != null ? source.getLandingId() : (source.getLanding() != null ? source.getLanding().getId() : null);
        if (copyIfNull || (landingId != null)) {
            target.setLanding(landingId == null ? null : load(Landing.class, landingId));
        }

        // Operation
        Integer operationId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
        if (copyIfNull || (operationId != null)) {
            target.setOperation(operationId == null ? null : load(Operation.class, operationId));
        }

        // Sale (in SIH, Sale is ExpectedSale and is linked also with Landing)
        Integer saleId = source.getSaleId() != null ? source.getSaleId() : (source.getSale() != null ? source.getSale().getId() : null);
        if (copyIfNull || (saleId != null)) {
            target.setSale(saleId == null ? null : load(Sale.class, saleId));
        }

        // Batch (link for sale on batch)
        Integer batchId = source.getBatchId() != null ? source.getBatchId() : (source.getBatch() != null ? source.getBatch().getId() : null);
        if (copyIfNull || (batchId != null)) {
            target.setBatch(batchId == null ? null : load(Batch.class, batchId));
        }

    }

    @Override
    public List<ProductVO> saveByOperationId(int operationId, @Nonnull List<ProductVO> products) {

        // Load parent entity
        Operation parent = find(Operation.class, operationId);

        products.forEach(source -> {
            source.setOperationId(operationId);
            source.setLandingId(null);
            source.setLanding(null);
            source.setSaleId(null);
            source.setSale(null);
            // set default weight method
            source.setWeightCalculated(false);
        });

        // Save all by parent
        return saveByParent(parent, products);
    }

    @Override
    public List<ProductVO> saveByLandingId(int landingId, @Nonnull List<ProductVO> products) {

        // Load parent entity
        Landing parent = find(Landing.class, landingId);

        products.forEach(source -> {
            source.setLandingId(landingId);
            source.setOperationId(null);
            source.setOperation(null);
            source.setSaleId(null);
            source.setSale(null);
        });

        // Save all by parent
        return saveByParent(parent, products);
    }

    @Override
    public List<ProductVO> saveBySaleId(int saleId, @Nonnull List<ProductVO> products) {

        // Load parent entity
        Sale parent = find(Sale.class, saleId);

        // Get landing Id (to optimize linked data for SIH)
        Integer landingId = Optional.ofNullable(parent.getTrip())
            .map(trip -> landingRepository.findAll(LandingFilterVO.builder().tripId(trip.getId()).build()))
            .filter(landings -> CollectionUtils.size(landings) == 1)
            .map(landings -> landings.get(0).getId())
            .orElse(null);

        products.forEach(source -> {
            source.setSaleId(saleId);
            source.setLandingId(landingId);
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

        if (product.getDressingId() != null && getPmfmIdByPmfmEnum(PmfmEnum.DRESSING) != null) {
            product.getMeasurementValues().put(getPmfmIdByPmfmEnum(PmfmEnum.DRESSING), product.getDressingId().toString());
        }
        if (product.getPreservationId() != null && getPmfmIdByPmfmEnum(PmfmEnum.PRESERVATION) != null) {
            product.getMeasurementValues().put(getPmfmIdByPmfmEnum(PmfmEnum.PRESERVATION), product.getPreservationId().toString());
        }
        if (product.getSizeCategoryId() != null && getPmfmIdByPmfmEnum(PmfmEnum.SIZE_CATEGORY) != null) {
            product.getMeasurementValues().put(getPmfmIdByPmfmEnum(PmfmEnum.SIZE_CATEGORY), product.getSizeCategoryId().toString());
        }
        if (product.getCost() != null && getPmfmIdByPmfmEnum(PmfmEnum.TOTAL_PRICE) != null) {
            product.getMeasurementValues().put(getPmfmIdByPmfmEnum(PmfmEnum.TOTAL_PRICE), product.getCost().toString());
        }

        product.getMeasurementValues().putAll(measurementDao.getProductSortingMeasurementsMap(product.getId()));
        product.getMeasurementValues().putAll(measurementDao.getProductQuantificationMeasurementsMap(product.getId()));

        // Sale specific :
        if (product.getSaleId() != null) {
            // Get average price per packaging and set it to generic average price pmfm
            String packagingId = product.getMeasurementValues().get(getPmfmIdByPmfmEnum(PmfmEnum.PACKAGING));
            if (StringUtils.isNotBlank(packagingId) && getPmfmIdByPmfmEnum(PmfmEnum.AVERAGE_PACKAGING_PRICE) != null) {
                QualitativeValue packaging = load(QualitativeValue.class, Integer.valueOf(packagingId));
                String avgPackagingPrice = product.getMeasurementValues().remove(getPmfmIdByLabel("AVERAGE_PRICE_" + packaging.getLabel()));
                if (StringUtils.isNotBlank(avgPackagingPrice)) {
                    // put it as generic avg price
                    product.getMeasurementValues().put(getPmfmIdByPmfmEnum(PmfmEnum.AVERAGE_PACKAGING_PRICE), avgPackagingPrice);
                }
            }

            // Convert sale ratio as percentage
            String ratioText = product.getMeasurementValues().get(getPmfmIdByPmfmEnum(PmfmEnum.SALE_RATIO));
            if (StringUtils.isNotBlank(ratioText)) {
                product.getMeasurementValues().put(getPmfmIdByPmfmEnum(PmfmEnum.SALE_RATIO), Double.toString((Double.parseDouble(ratioText) * 100)));
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
                if (product.getSaleId() != null) {

                    // Replace average price per packaging by packaging depending average price
                    String packagingId = product.getMeasurementValues().get(getPmfmIdByPmfmEnum(PmfmEnum.PACKAGING));
                    String averagePrice = product.getMeasurementValues().get(getPmfmIdByPmfmEnum(PmfmEnum.AVERAGE_PACKAGING_PRICE));
                    if (StringUtils.isNotBlank(packagingId) && StringUtils.isNotBlank(averagePrice)) {
                        QualitativeValue packaging = load(QualitativeValue.class, Integer.valueOf(packagingId));
                        Integer averagePriceForPackingPmfmId = getPmfmIdByLabel("AVERAGE_PRICE_" + packaging.getLabel());
                        if (averagePriceForPackingPmfmId != null) {
                            // put it as specified packaging avg price
                            product.getMeasurementValues().put(averagePriceForPackingPmfmId, product.getMeasurementValues().remove(getPmfmIdByPmfmEnum(PmfmEnum.AVERAGE_PACKAGING_PRICE)));
                        }
                    }

                    // Sale ratio must be converted from percentage
                    String ratioText = product.getMeasurementValues().get(getPmfmIdByPmfmEnum(PmfmEnum.SALE_RATIO));
                    if (StringUtils.isNotBlank(ratioText)) {
                        product.getMeasurementValues().put(getPmfmIdByPmfmEnum(PmfmEnum.SALE_RATIO), Double.toString((Double.parseDouble(ratioText) / 100)));
                    }

                    // If rank order is present on a batch (=packet) produce sale, add rank order as sale rank order measurement
                    if (product.getRankOrder() != null && product.getBatchId() != null) {
                        product.getMeasurementValues().put(getPmfmIdByPmfmEnum(PmfmEnum.SALE_RANK_ORDER), product.getRankOrder().toString());
                    }

                }

                Map<Integer, String> quantificationMeasurements = Maps.newLinkedHashMap();
                Map<Integer, String> sortingMeasurements = Maps.newLinkedHashMap();
                product.getMeasurementValues().forEach((pmfmId, value) -> {
                    if (isWeightPmfm(pmfmId)) {
                        quantificationMeasurements.putIfAbsent(pmfmId, value);
                    } else {
                        if (sortingMeasurements.containsKey(pmfmId)) {
                            LOG.warn(String.format("Duplicate measurement width {pmfmId: %s} on product {id: %s}", pmfmId, product.getId()));
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
                result = load(QualitativeValue.class, Integer.parseInt(value));
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

    private Integer getPmfmIdByPmfmEnum(PmfmEnum pmfmEnum) {
        return getPmfmIdByLabel(pmfmEnum.getLabel());
    }

    private Integer getPmfmIdByLabel(@Nonnull String label) {
        pmfmIdByLabel.putIfAbsent(label, pmfmRepository.findByLabel(label).map(PmfmVO::getId).orElse(null));
        return pmfmIdByLabel.get(label);
    }

    public Integer getMeasuredMethodId() {
        if (measuredMethodId == null) {
            measuredMethodId = referentialDao.findByUniqueLabel(Method.class.getSimpleName(), MethodEnum.MEASURED_BY_OBSERVER.getLabel())
                .map(ReferentialVO::getId).orElseThrow(IllegalStateException::new);
        }
        return measuredMethodId;
    }

    public Integer getCalculatedMethodId() {
        if (calculatedMethodId == null) {
            calculatedMethodId = referentialDao.findByUniqueLabel(Method.class.getSimpleName(), MethodEnum.CALCULATED.getLabel())
                .map(ReferentialVO::getId).orElseThrow(IllegalStateException::new);
        }
        return calculatedMethodId;
    }
}
