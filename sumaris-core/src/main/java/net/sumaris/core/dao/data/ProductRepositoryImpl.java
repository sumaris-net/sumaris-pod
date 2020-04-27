package net.sumaris.core.dao.data;

import com.google.common.collect.Maps;
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.pmfm.PmfmDao;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.pmfm.Method;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ProductVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.filter.ProductFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
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
    extends DataRepositoryImpl<Product, Integer, ProductVO, ProductFilterVO>
    implements ProductRepositoryExtend {

    private static final Logger LOG = LoggerFactory.getLogger(ProductRepositoryImpl.class);

    private final ReferentialDao referentialDao;
    private final PersonDao personDao;
    private final LandingRepository landingRepository;
    private final PmfmDao pmfmDao;
    private final MeasurementDao measurementDao;

    private final Map<PmfmEnum, Integer> pmfmMap;

    @Autowired
    public ProductRepositoryImpl(EntityManager entityManager,
                                 ReferentialDao referentialDao,
                                 PersonDao personDao,
                                 PmfmDao pmfmDao,
                                 LandingRepository landingRepository,
                                 MeasurementDao measurementDao) {
        super(Product.class, entityManager);
        this.referentialDao = referentialDao;
        this.personDao = personDao;
        this.landingRepository = landingRepository;
        this.pmfmDao = pmfmDao;
        this.measurementDao = measurementDao;

        setCheckUpdateDate(false);

        pmfmMap = new HashMap<>();
    }

    @Override
    public Class<ProductVO> getVOClass() {
        return ProductVO.class;
    }

    @Override
    public Specification<Product> toSpecification(ProductFilterVO filter) {
        if (filter == null) return null;

        return Specification.where(hasLandingId(filter.getLandingId()))
            .and(hasOperationId(filter.getOperationId()))
            .and(hasSaleId(filter.getSaleId()));
    }

    @Override
    public void toVO(Product source, ProductVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Recorder person
        if ((fetchOptions == null || fetchOptions.isWithRecorderPerson()) && source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
        }

        if (source.getTaxonGroup() != null) {
            target.setTaxonGroup(referentialDao.toReferentialVO(source.getTaxonGroup()));
        }
        if (source.getSaleType() != null) {
            target.setSaleType(referentialDao.toReferentialVO(source.getSaleType()));
        }
        if (source.getWeightMethod() != null) {
            target.setWeightMethod(referentialDao.toReferentialVO(source.getWeightMethod()));
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

        // Weight method
        if (copyIfNull || source.getWeightMethod() != null) {
            if (source.getWeightMethod() == null || source.getWeightMethod().getId() == null) {
                target.setWeightMethod(null);
            } else {
                target.setWeightMethod(load(Method.class, source.getWeightMethod().getId()));
            }
        }

        // Dressing
        QualitativeValue dressing = extractSortingQualitativeValue(source, getPmfmIdByPmfmEnum(PmfmEnum.DRESSING));
        if (copyIfNull || dressing != null) {
            target.setDressing(dressing);
        }

        // Preservation
        QualitativeValue preservation = extractSortingQualitativeValue(source, getPmfmIdByPmfmEnum(PmfmEnum.PRESERVATION));
        if (copyIfNull || preservation != null) {
            target.setPreservation(preservation);
        }

        // Size Category
        QualitativeValue sizeCategory = extractSortingQualitativeValue(source, getPmfmIdByPmfmEnum(PmfmEnum.SIZE_CATEGORY));
        if (copyIfNull || sizeCategory != null) {
            target.setSizeCategory(sizeCategory);
        }

        // Cost
        Double cost = extractSortingNumericalValue(source, getPmfmIdByPmfmEnum(PmfmEnum.TOTAL_PRICE));
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
        Operation parent = get(Operation.class, operationId);

        products.forEach(source -> {
            source.setOperationId(operationId);
            source.setLandingId(null);
            source.setLanding(null);
            source.setSaleId(null);
            source.setSale(null);
        });

        // Save all by parent
        return saveByParent(parent, products);
    }

    @Override
    public List<ProductVO> saveByLandingId(int landingId, @Nonnull List<ProductVO> products) {

        // Load parent entity
        Landing parent = get(Landing.class, landingId);

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
        Sale parent = get(Sale.class, saleId);

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

                Map<Integer, String> quantificationMeasurements = Maps.newLinkedHashMap();
                Map<Integer, String> sortingMeasurements = Maps.newLinkedHashMap();
                product.getMeasurementValues().forEach((pmfmId, value) -> {
                    if (isWeightPmfm(pmfmId)) {
                        quantificationMeasurements.putIfAbsent(pmfmId, value);
                    }
                    else {
                        if (sortingMeasurements.containsKey(pmfmId)) {
                            LOG.warn(String.format("Duplicate measurement width {pmfmId: %s} on product {id: %s}", pmfmId, product.getId()));
                        }
                        else {
                            sortingMeasurements.putIfAbsent(pmfmId, value);
                        }
                    }
                });
                measurementDao.saveProductSortingMeasurementsMap(product.getId(), sortingMeasurements);
                measurementDao.saveProductQuantificationMeasurementsMap(product.getId(), quantificationMeasurements);
            }
        });

    }

    private QualitativeValue extractSortingQualitativeValue(ProductVO source, Integer pmfmId) {

        QualitativeValue result = null;

        if (MapUtils.isNotEmpty(source.getMeasurementValues()) && pmfmId != null) {

            String value = source.getMeasurementValues().remove(pmfmId);

            if (value != null) {
                result = load(QualitativeValue.class, Integer.parseInt(value));
            }
        }

        return result;
    }

    private Double extractSortingNumericalValue(ProductVO source, Integer pmfmId) {
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
        return pmfmDao.hasLabelSuffix(pmfmId, "WEIGHT");
    }

    private Integer getPmfmIdByPmfmEnum(PmfmEnum pmfmEnum) {
        pmfmMap.putIfAbsent(pmfmEnum, pmfmDao.findByLabel(pmfmEnum.getLabel()).map(PmfmVO::getId).orElse(null));
        return pmfmMap.get(pmfmEnum);
    }
}
