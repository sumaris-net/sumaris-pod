package net.sumaris.core.dao.data;

import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.referential.PmfmDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.pmfm.Method;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.MeasurementVO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author peck7 on 30/03/2020.
 */
public class ProductRepositoryImpl
    extends DataRepositoryImpl<Product, Integer, ProductVO, ProductFilterVO>
    implements ProductRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ProductRepositoryImpl.class);

    private final ReferentialDao referentialDao;
    private final PersonDao personDao;
    private final TaxonGroupRepository taxonGroupRepository;
    private final LandingRepository landingRepository;
    private final PmfmDao pmfmDao;

    @Autowired
    public ProductRepositoryImpl(EntityManager entityManager,
                                 ReferentialDao referentialDao,
                                 PersonDao personDao,
                                 TaxonGroupRepository taxonGroupRepository,
                                 LandingRepository landingRepository,
                                 PmfmDao pmfmDao) {
        super(Product.class, entityManager);
        this.referentialDao = referentialDao;
        this.personDao = personDao;
        this.taxonGroupRepository = taxonGroupRepository;
        this.landingRepository = landingRepository;
        this.pmfmDao = pmfmDao;
    }

    @Override
    public Class<ProductVO> getVOClass() {
        return ProductVO.class;
    }

    @Override
    public Specification<Product> toSpecification(ProductFilterVO filter) {
        if (filter == null) return null;

        return Specification.where(
            and(
                hasLandingId(filter.getLandingId()),
                hasOperationId(filter.getOperationId()),
                hasSaleId(filter.getSaleId())
            )
        );
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
        if (source.getDressing() != null) {
            addSortingMeasurement(target, PmfmEnum.DRESSING, source.getDressing());
        }
        if (source.getPreservation() != null) {
            addSortingMeasurement(target, PmfmEnum.PRESERVATION, source.getPreservation());
        }
        if (source.getSizeCategory() != null) {
            addSortingMeasurement(target, PmfmEnum.SIZE_CATEGORY, source.getSizeCategory());
        }
        if (source.getCost() != null) {
            addSortingMeasurement(target, PmfmEnum.TOTAL_PRICE, source.getCost());
        }

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
        QualitativeValue dressing = extractSortingQualitativeValue(source, PmfmEnum.DRESSING);
        if (copyIfNull || dressing != null) {
            target.setDressing(dressing);
        }

        // Preservation
        QualitativeValue preservation = extractSortingQualitativeValue(source, PmfmEnum.PRESERVATION);
        if (copyIfNull || preservation != null) {
            target.setPreservation(preservation);
        }

        // Size Category
        QualitativeValue sizeCategory = extractSortingQualitativeValue(source, PmfmEnum.SIZE_CATEGORY);
        if (copyIfNull || sizeCategory != null) {
            target.setSizeCategory(sizeCategory);
        }

        // Cost
        Double cost = extractSortingNumericalValue(source, PmfmEnum.TOTAL_PRICE);
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

    protected List<ProductVO> saveByParent(@Nonnull IWithProductsEntity<Integer, Product> parent, @Nonnull List<ProductVO> products) {

        // Load existing entities
        Set<Integer> existingProductIds = parent.getProducts().stream().map(Product::getId).collect(Collectors.toSet());

        products.forEach(product -> {
            save(product);
            existingProductIds.remove(product.getId());
        });

        // Delete remaining
        existingProductIds.forEach(this::deleteById);

        return products;
    }

    private void addSortingMeasurement(ProductVO target, PmfmEnum pmfmEnum, QualitativeValue qualitativeValue) {
        MeasurementVO sortingMeasurement = createSortingMeasurement(target, pmfmEnum);
        sortingMeasurement.setQualitativeValue(referentialDao.toReferentialVO(qualitativeValue));
    }

    private void addSortingMeasurement(ProductVO target, PmfmEnum pmfmEnum, Number value) {
        MeasurementVO sortingMeasurement = createSortingMeasurement(target, pmfmEnum);
        sortingMeasurement.setNumericalValue(value.doubleValue());
    }

    private MeasurementVO createSortingMeasurement(ProductVO target, PmfmEnum pmfmEnum) {
        MeasurementVO sortingMeasurement = new MeasurementVO();
        sortingMeasurement.setEntityName(ProductSortingMeasurement.class.getSimpleName());

        PmfmVO pmfm = pmfmDao.getByLabel(pmfmEnum.getLabel());
        sortingMeasurement.setPmfmId(pmfm.getId());

        if (target.getSortingMeasurements() == null) {
            target.setSortingMeasurements(new ArrayList<>());
        }
        target.getSortingMeasurements().add(sortingMeasurement);

        // affect fake id
        sortingMeasurement.setId(-target.getSortingMeasurements().size());

        return sortingMeasurement;
    }

    private QualitativeValue extractSortingQualitativeValue(ProductVO source, PmfmEnum pmfmEnum) {

        int pmfmId = pmfmDao.getByLabel(pmfmEnum.getLabel()).getId();
        QualitativeValue result = null;

        if (CollectionUtils.isNotEmpty(source.getSortingMeasurements())) {

            MeasurementVO measurement = source.getSortingMeasurements().stream()
                .filter(m -> m.getPmfmId() == pmfmId && m.getQualitativeValue() != null)
                .findFirst()
                .orElse(null);

            if (measurement != null) {
                result = load(QualitativeValue.class, measurement.getQualitativeValue().getId());
                source.getSortingMeasurements().remove(measurement);
            }
        }

        if (MapUtils.isNotEmpty(source.getMeasurementValues())) {

            String value = source.getMeasurementValues().remove(pmfmId);

            if (value != null && result == null) {
                result = load(QualitativeValue.class, Integer.parseInt(value));
            }
        }

        return result;
    }

    private Double extractSortingNumericalValue(ProductVO source, PmfmEnum pmfmEnum) {
        int pmfmId = pmfmDao.getByLabel(pmfmEnum.getLabel()).getId();
        Double result = null;

        if (CollectionUtils.isNotEmpty(source.getSortingMeasurements())) {

            MeasurementVO measurement = source.getSortingMeasurements().stream()
                .filter(m -> m.getPmfmId() == pmfmId)
                .findFirst()
                .orElse(null);

            if (measurement != null) {
                result = measurement.getNumericalValue();
                source.getSortingMeasurements().remove(measurement);
            }
        }

        if (MapUtils.isNotEmpty(source.getMeasurementValues())) {

            String value = source.getMeasurementValues().remove(pmfmId);

            if (value != null && result == null) {
                result = Double.parseDouble(value);
            }
        }

        return result;
    }
}
