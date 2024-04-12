/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.dao.data.batch;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.pmfm.ParameterRepository;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.DenormalizedBatch;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Numbers;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.QuantificationMeasurementVO;
import net.sumaris.core.vo.data.batch.*;
import net.sumaris.core.vo.referential.ParameterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author peck7 on 09/06/2020.
 */
@Slf4j
public class DenormalizedBatchRepositoryImpl
    extends SumarisJpaRepositoryImpl<DenormalizedBatch, Integer, DenormalizedBatchVO>
    implements DenormalizedBatchSpecifications<DenormalizedBatch, DenormalizedBatchVO> {

    private final SumarisConfiguration config;
    private final ReferentialDao referentialDao;
    private final PmfmRepository pmfmRepository;
    private final ParameterRepository parameterRepository;

    private final TaxonNameRepository taxonNameRepository;

    private final ApplicationContext applicationContext;

    private final DenormalizedBatchSortingValueRepository sortingValueRepository;

    public DenormalizedBatchRepositoryImpl(EntityManager entityManager,
                                           SumarisConfiguration config,
                                           ReferentialDao referentialDao,
                                           PmfmRepository pmfmRepository,
                                           ParameterRepository parameterRepository,
                                           TaxonNameRepository taxonNameRepository,
                                           DenormalizedBatchSortingValueRepository sortingValueRepository,
                                           ApplicationContext applicationContext) {
        super(DenormalizedBatch.class, entityManager);
        this.config = config;
        this.referentialDao = referentialDao;
        this.pmfmRepository = pmfmRepository;
        this.parameterRepository = parameterRepository;
        this.taxonNameRepository = taxonNameRepository;
        this.sortingValueRepository = sortingValueRepository;
        this.applicationContext = applicationContext;
    }

    public Optional<DenormalizedBatchVO> findById(int id, DenormalizedBatchFetchOptions fetchOptions) {
        return super.findById(id).map(entity -> toVO(entity, fetchOptions));
    }

    public List<DenormalizedBatchVO> findAll(DenormalizedBatchesFilterVO filter, DenormalizedBatchFetchOptions fetchOptions) {
        return findAll(toSpecification(filter))
                .stream()
                .map(e -> this.toVO(e, fetchOptions))
//                .sorted(Comparator.comparing(DenormalizedBatchVO::getOperationId).thenComparing(Comparator.comparing(DenormalizedBatchVO::getFlatRankOrder)))
                .collect(Collectors.toList());
    }

    public List<DenormalizedBatchVO> findAll(
            DenormalizedBatchesFilterVO filter,
            int offset,
            int size,
            String sortAttribute,
            SortDirection sortDirection,
            DenormalizedBatchFetchOptions fetchOptions
    ) {
        Specification<DenormalizedBatch> spec = filter != null ? toSpecification(filter) : null;
        TypedQuery<DenormalizedBatch> query = getQuery(spec, offset, size, sortAttribute, sortDirection, getDomainClass());
        try (Stream<DenormalizedBatch> stream = streamQuery(query)) {
            return stream.map(entity -> toVO(entity, fetchOptions)).toList();
        }
    }

    public long count(DenormalizedBatchesFilterVO filter) {
        return count(toSpecification(filter));
    }

    @Override
    public Class<DenormalizedBatchVO> getVOClass() {
        return DenormalizedBatchVO.class;
    }

    @Override
    public void toVO(DenormalizedBatch source, DenormalizedBatchVO target, boolean copyIfNull) {
        toVO(source, target, DenormalizedBatchFetchOptions.DEFAULT, copyIfNull);
    }

    public void toVO(DenormalizedBatch source, DenormalizedBatchVO target, DenormalizedBatchFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        // Quality flag
        target.setQualityFlagId(source.getQualityFlag().getId());

        if (source.getTaxonGroup() != null) {
            target.setTaxonGroup(referentialDao.toVO(source.getTaxonGroup()));
        }

        // Taxon name (from reference)
        if (source.getReferenceTaxon() != null && source.getReferenceTaxon().getId() != null) {
            target.setTaxonName(taxonNameRepository.findReferentByReferenceTaxonId(source.getReferenceTaxon().getId()).orElse(null));
        }

        // Sorting values
        if (source.getSortingValues() != null) {
            // As List
            if (fetchOptions == null || fetchOptions.isWithChildrenEntities()) {
                List<DenormalizedBatchSortingValueVO> sortingValues = source.getSortingValues()
                        .stream()
                        .map(sortingValue -> sortingValueRepository.toVO(sortingValue))
                        .collect(Collectors.toList());
                target.setSortingValues(sortingValues);
            }
        }

        // Operation
        Integer operationId = source.getOperation() != null ? source.getOperation().getId() : null;
        if (copyIfNull || operationId != null) {
            target.setOperationId(operationId);
        }

        // Sale
        Integer saleId = source.getSale() != null ? source.getSale().getId() : null;
        if (copyIfNull || saleId != null) {
            target.setSaleId(saleId);
        }

        Integer parentId = source.getParent() != null ? source.getParent().getId() : null;
        if (copyIfNull || parentId != null) {
            target.setParentId(parentId);
        }

        // TODO handle from fetchOptions
        // - withChildrenEntities
        // - withMeasurementValues
    }

    @Override
    public void toEntity(DenormalizedBatchVO source, DenormalizedBatch target, boolean copyIfNull) {
        // Always same id
        target.setId(source.getId());

        super.toEntity(source, target, copyIfNull);

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(getReference(QualityFlag.class, config.getDefaultQualityFlagId()));
            } else {
                target.setQualityFlag(getReference(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        // Parent operation
        {
            Integer operationId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
            source.setOperationId(operationId);
            if (copyIfNull || (operationId != null)) {
                if (operationId == null) {
                    target.setOperation(null);
                } else {
                    target.setOperation(getReference(Operation.class, operationId));
                }
            }
        }

        // Parent sale
        {
            Integer saleId = source.getSaleId() != null ? source.getSaleId() : (source.getSale() != null ? source.getSale().getId() : null);
            source.setSaleId(saleId);
            if (copyIfNull || (saleId != null)) {
                if (saleId == null) {
                    target.setSale(null);
                } else {
                    target.setSale(getReference(Sale.class, saleId));
                }
            }
        }

        // Taxon group
        {
            Integer taxonGroupId = source.getTaxonGroup() != null ? source.getTaxonGroup().getId() : null;
            if (copyIfNull || taxonGroupId != null) {
                if (taxonGroupId == null) {
                    target.setTaxonGroup(null);
                } else {
                    target.setTaxonGroup(getReference(TaxonGroup.class, taxonGroupId));
                }
            }
        }

        // Inherited taxon group
        {
            Integer inheritedTaxonGroupId = source.getInheritedTaxonGroup() != null ? source.getInheritedTaxonGroup().getId() : null;
            if (copyIfNull || inheritedTaxonGroupId != null) {
                if (inheritedTaxonGroupId == null) {
                    target.setInheritedTaxonGroup(null);
                } else {
                    target.setInheritedTaxonGroup(getReference(TaxonGroup.class, inheritedTaxonGroupId));
                }
            }
        }

        // Taxon name
        {
            Integer taxonNameId = source.getTaxonName() != null ? source.getTaxonName().getId() : null;
            if (copyIfNull || taxonNameId != null) {
                if (taxonNameId == null) {
                    target.setReferenceTaxon(null);
                } else {
                    Integer referenceTaxonId = taxonNameRepository.getReferenceTaxonIdById(taxonNameId);
                    target.setReferenceTaxon(getReference(ReferenceTaxon.class, referenceTaxonId));
                }
            }
        }

        // Inherited taxon name
        {
            Integer inheritedTaxonNameId = source.getInheritedTaxonName() != null ? source.getInheritedTaxonName().getId() : null;
            if (copyIfNull || inheritedTaxonNameId != null) {
                if (inheritedTaxonNameId == null) {
                    target.setInheritedReferenceTaxon(null);
                } else {
                    Integer referenceTaxonId = taxonNameRepository.getReferenceTaxonIdById(inheritedTaxonNameId);
                    target.setInheritedReferenceTaxon(getReference(ReferenceTaxon.class, referenceTaxonId));
                }
            }
        }

        // Calculated taxon group
        {
            Integer calculatedTaxonGroupId = source.getCalculatedTaxonGroup() != null ? source.getCalculatedTaxonGroup().getId() : null;
            if (copyIfNull || calculatedTaxonGroupId != null) {
                if (calculatedTaxonGroupId == null) {
                    target.setCalculatedTaxonGroup(null);
                } else {
                    target.setCalculatedTaxonGroup(getReference(TaxonGroup.class, calculatedTaxonGroupId));
                }
            }
        }

        // Parent name
        {
            Integer parentBatchId = source.getParent() != null ? source.getParent().getId() : source.getParentId();
            if (copyIfNull || parentBatchId != null) {
                if (parentBatchId == null) {
                    target.setParent(null);
                } else {
                    target.setParent(getReference(DenormalizedBatch.class, parentBatchId));
                }
            }
        }
    }

    @Override
    protected void onAfterSaveEntity(DenormalizedBatchVO vo, DenormalizedBatch savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);

        List<Integer> existingSvIds = Beans.collectIds(savedEntity.getSortingValues());

        Beans.getStream(vo.getSortingValues())
            .forEach(source -> {
                source.setBatchId(vo.getId());
                sortingValueRepository.save(source);
                existingSvIds.remove(source.getId());
            });

        if (CollectionUtils.isNotEmpty(existingSvIds)) {
            sortingValueRepository.deleteAllByIdInBatch(existingSvIds);
        }
    }

    @Override
    public DenormalizedBatchVO getCatchBatchByOperationId(int operationId) {
        return findOne(hasNoParent()
            .and(hasOperationId(operationId)))
            .map(this::toVO)
            .orElse(null);
    }

    @Override
    public DenormalizedBatchVO getCatchBatchBySaleId(int saleId) {
        return findOne(hasNoParent()
            .and(hasSaleId(saleId)))
            .map(this::toVO)
            .orElse(null);
    }

    @Override
    public List<DenormalizedBatchVO> saveAllByOperationId(int operationId, @Nonnull List<DenormalizedBatchVO> sources) {

        // Set parent link
        sources.forEach(b -> b.setOperationId(operationId));

        // Get existing ids
        Set<Integer> existingIdsToRemove = getRepository().getAllIdByOperationId(operationId);

        // Save
        sources.forEach(source -> {
            save(source);
            existingIdsToRemove.remove(source.getId());
        });

        // Delete remaining objects
        if (CollectionUtils.isNotEmpty(existingIdsToRemove)) {
            deleteAllById(existingIdsToRemove);
        }

        return sources;
    }

    @Override
    public List<DenormalizedBatchVO> saveAllBySaleId(int saleId, @Nonnull List<DenormalizedBatchVO> sources) {

        // Set parent link
        sources.forEach(b -> b.setSaleId(saleId));

        // Get existing fishing areas
        Set<Integer> existingIdsToRemove = getRepository().getAllIdBySaleId(saleId);

        // Save
        sources.forEach(b -> {
            save(b);
            existingIdsToRemove.remove(b.getId());
        });

        // Delete remaining objects
        if (CollectionUtils.isNotEmpty(existingIdsToRemove)) {
            deleteAllById(existingIdsToRemove);
        }

        return sources;
    }

    @Override
    public void deleteAllById(@NonNull Iterable<? extends Integer> ids) {

        if (!ids.iterator().hasNext()) return;

        // Use query, instead of default JPA implements
        // => Avoid an error when the denormalized_batch to remove not exists
        sortingValueRepository.deleteAllByBatchId(ids);
        getRepository().deleteAllById(ids);
    }

    public DenormalizedBatch toEntity(DenormalizedBatchVO source) {
        Preconditions.checkNotNull(source);

        // Try to load by id, or create a new entity
        DenormalizedBatch target =  Optional.ofNullable(source.getId())
            .flatMap(this::findById)
            .orElseGet(this::createEntity);

        toEntity(source, target, true);

        return target;
    }

    @Override
    public <S extends DenormalizedBatch> S save(S entity) {
        this.getSession().saveOrUpdate(entity);
        return entity;
    }

    @Override
    public DenormalizedBatchVO toVO(DenormalizedBatch source) {
        if (source == null) return null;
        DenormalizedBatchVO target = createVO();
        toVO(source, target, true);
        return target;
    }

    public DenormalizedBatchVO toVO(DenormalizedBatch source, DenormalizedBatchFetchOptions fetchOptions) {
        return this.toVO(source);
    }

    /* -- protected methods -- */

    protected Specification<DenormalizedBatch> toSpecification(DenormalizedBatchesFilterVO filter) {
        // default specification
        return BindableSpecification
                .where(hasTripId(filter.getTripId()))
                .and(hasOperationId(filter.getOperationId()))
                .and(isLanding(filter.getIsLanding()))
                .and(isDiscard(filter.getIsDiscard()));
    }

    @Override
    public void copy(BatchVO source, DenormalizedBatchVO target, boolean copyIfNull) {
        Beans.copyProperties(source, target);

        // Quality flag (default value)
        if (source.getQualityFlagId() == null) {
            target.setQualityFlagId(QualityFlagEnum.NOT_QUALIFIED.getId());
        }

        // Init sorting value rank order (must be high, to let inherited values BEFORE current )
        MutableInt sortingValueRankOrder = new MutableInt( 10000);

        if (source.getMeasurementValues() != null) {
            Set<Integer> pmfmIds = source.getMeasurementValues().keySet();

            // Init rankOrder with a very high value, inherited values must be BEFORE current values
            pmfmIds.forEach(pmfmId -> {
                String valStr = source.getMeasurementValues().get(pmfmId);
                if (StringUtils.isBlank(valStr)) return; // Skip

                // Weight
                if (this.isWeightPmfm(pmfmId)) {
                    Double weight = Double.parseDouble(valStr);
                    target.setWeight(weight);
                    target.setWeightMethodId(getPmfmMethodId(pmfmId));
                }

                // landing / discard
                else {
                    if (pmfmId == PmfmEnum.DISCARD_OR_LANDING.getId()) {
                        Integer qvId = Integer.parseInt(valStr);
                        target.setIsLanding(Objects.equals(qvId, QualitativeValueEnum.LANDING.getId()));
                        target.setIsDiscard(Objects.equals(qvId, QualitativeValueEnum.DISCARD.getId()));
                    }

                    // Any other sorting value
                    sortingValueRankOrder.increment();
                    DenormalizedBatchSortingValueVO sv = toSortingValueVO(pmfmId, valStr, sortingValueRankOrder.intValue());
                    if (sv == null) {
                        throw new SumarisTechnicalException(String.format("Unable to convert sorting value, in batch {id: %s, label: '%s'}. Pmfm {id: %s}",
                                target.getId(), target.getLabel(), pmfmId));
                    }
                    target.addSortingValue(sv);
                }
            });

        }
        else {
            // weight;
            if (source.getQuantificationMeasurements() != null) {
                source.getQuantificationMeasurements().stream()
                        .filter(QuantificationMeasurementVO::getIsReferenceQuantification)
                        .forEach(m -> {
                            Double weight = m.getNumericalValue();
                            if (weight != null) {
                                target.setWeight(weight);
                                target.setWeightMethodId(getPmfmMethodId(m.getPmfmId()));
                            }
                        });
            }

            if (source.getSortingMeasurements() != null) {
                source.getSortingMeasurements().forEach(m -> {
                    int pmfmId = m.getPmfmId();
                    if (pmfmId == PmfmEnum.DISCARD_OR_LANDING.getId()) {
                        Integer qvId = m.getQualitativeValue() != null ? m.getQualitativeValue().getId() : null;
                        target.setIsLanding(Objects.equals(qvId, QualitativeValueEnum.LANDING.getId()));
                        target.setIsDiscard(Objects.equals(qvId, QualitativeValueEnum.DISCARD.getId()));
                    }

                    // Any other sorting value
                    DenormalizedBatchSortingValueVO sv = toSortingValueVO(pmfmId, m, sortingValueRankOrder.intValue() + m.getRankOrder());
                    if (sv == null) {
                        throw new SumarisTechnicalException(String.format("Unable to convert sorting value, in batch {id: %s, label: '%s'}. Pmfm {id: %s}",
                                target.getId(), target.getLabel(), pmfmId));
                    }
                    target.addSortingValue(sv);
                });
            }
        }

        // Sorting value text
        if (copyIfNull || CollectionUtils.isNotEmpty(target.getSortingValues())) {
            String sortingValuesText = computeSortingValuesText(target.getSortingValues());
            target.setSortingValuesText(sortingValuesText);
        }

        // Taxon group
        if (copyIfNull || source.getTaxonGroup() != null) {
            target.setTaxonGroup(source.getTaxonGroup());
            target.setInheritedTaxonGroup(target.getTaxonGroup());
        }

        // Taxon name
        if (copyIfNull || source.getTaxonName() != null) {
            target.setTaxonName(source.getTaxonName());
            target.setInheritedTaxonName(source.getTaxonName());
        }

        // Link to parent: operation or sale
        if (copyIfNull || source.getOperationId() != null) {
            target.setOperationId(source.getOperationId());
        }
        if (copyIfNull || source.getSaleId() != null) {
            target.setOperationId(source.getSaleId());
        }

    }

    protected boolean isWeightPmfm(int pmfmId) {
        return pmfmRepository.hasLabelSuffix(pmfmId, "WEIGHT");
    }

    protected PmfmVO getPmfm(int pmfmId) {
        return pmfmRepository.findVOById(pmfmId)
            .orElseThrow(() -> new DataRetrievalFailureException("Cannot find PMFM with id=" + pmfmId));
    }

    protected Integer getPmfmMethodId(int pmfmId) {
        PmfmVO pmfm = pmfmRepository.get(pmfmId);
        return pmfm != null ? pmfm.getMethodId() : null;
    }

    protected DenormalizedBatchSortingValueVO toSortingValueVO(int pmfmId,
                                                               String valStr,
                                                               int rankOrder) {
        PmfmVO pmfm = getPmfm(pmfmId);
        PmfmValueType pmfmType = PmfmValueType.fromPmfm(pmfm);
        ParameterVO parameter = parameterRepository.get(pmfm.getParameterId());

        DenormalizedBatchSortingValueVO target = new DenormalizedBatchSortingValueVO();
        target.setRankOrder(rankOrder);
        target.setIsInherited(false);
        target.setPmfmId(pmfmId);
        target.setPmfm(pmfm);
        target.setParameter(parameter);
        target.setUnit(ReferentialVO.builder()
                .id(pmfm.getUnitId())
                .label(pmfm.getUnitLabel())
                .build());

        // Value
        if (pmfmType == PmfmValueType.DOUBLE || pmfmType == PmfmValueType.INTEGER) {
            target.setNumericalValue(Double.parseDouble(valStr));
        }
        else if (pmfmType == PmfmValueType.BOOLEAN) {
            target.setNumericalValue("true".equalsIgnoreCase(valStr) || "1".equals(valStr) ? 1d : 0d);
        }
        else if (pmfmType == PmfmValueType.STRING || pmfmType == PmfmValueType.DATE) {
            target.setAlphanumericalValue(valStr);
        }
        else if (pmfmType == PmfmValueType.QUALITATIVE_VALUE) {
            Integer qvId = Integer.parseInt(valStr);
            ReferentialVO qv = Beans.getStream(pmfm.getQualitativeValues())
                    .filter(item -> qvId.equals(item.getId()))
                    .findFirst().orElse(ReferentialVO.builder()
                    .id(Integer.parseInt(valStr))
                    .label("?")
                    .build());
            target.setQualitativeValue(qv);
        }
        else {
            return null;
        }


        return target;
    }

    protected DenormalizedBatchSortingValueVO toSortingValueVO(int pmfmId,
                                                               MeasurementVO measurement,
                                                               int rankOrder) {
        PmfmVO pmfm = getPmfm(pmfmId);
        ParameterVO parameter = parameterRepository.get(pmfm.getParameterId());

        DenormalizedBatchSortingValueVO target = new DenormalizedBatchSortingValueVO();
        target.setRankOrder(rankOrder);
        target.setIsInherited(false);
        target.setPmfmId(pmfmId);
        target.setPmfm(pmfm);
        target.setParameter(parameter);
        target.setUnit(ReferentialVO.builder()
                .id(pmfm.getUnitId())
                .label(pmfm.getUnitLabel())
                .build());

        // Value
        target.setNumericalValue(measurement.getNumericalValue());
        target.setAlphanumericalValue(measurement.getAlphanumericalValue());
        target.setQualitativeValue(measurement.getQualitativeValue());

        return target;
    }

    protected String computeSortingValuesText(Collection<DenormalizedBatchSortingValueVO> sources) {
        return Beans.getStream(sources)
                .map(this::getSortingValueText)
                .collect(Collectors.joining(", "));
    }

    protected String getSortingValueText(DenormalizedBatchSortingValueVO source) {

        StringBuilder result = new StringBuilder();

        // Parameter
        String parameterName = source.getParameter().getName();
        if (parameterName.matches(".*\\([A-Z]+\\)")) {
            parameterName = parameterName.replaceAll(".*\\(([A-Z]+)\\)", "$1=");
            result.append(parameterName);
        }

        // Value
        if (source.getNumericalValue() != null) {
            if (source.getPmfm().getMaximumNumberDecimals() != null) {
                String value = Numbers.format(source.getNumericalValue(), source.getPmfm().getMaximumNumberDecimals());
                result.append(value);
            }
            else {
                result.append(Numbers.format(source.getNumericalValue()));
            }

            // Unit label (=symbol)
            if (source.getUnit() != null && !Objects.equals(source.getUnit().getId(), UnitEnum.NONE.getId())) {
                result.append(" ").append(source.getUnit().getLabel());
            }
        }
        else if (source.getAlphanumericalValue() != null) {
            result.append(source.getAlphanumericalValue());
        }
        else if (source.getQualitativeValue() != null) {
            ReferentialVO qv = source.getQualitativeValue();
            result.append(qv.getLabel());
            //result.append(" - ").append(qv.getName());
        }


        return result.toString();
    }

    private DenormalizedBatchRepository repository;
    protected DenormalizedBatchRepository getRepository() {
        if (repository == null) {
            repository = this.applicationContext.getBean(DenormalizedBatchRepository.class);
        }
        return repository;
    }
}
