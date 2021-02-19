package net.sumaris.core.dao.data;

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
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.dao.technical.model.TreeNodeEntities;
import net.sumaris.core.model.data.DenormalizedBatch;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.DenormalizedBatchVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.QuantificationMeasurementVO;
import net.sumaris.core.vo.referential.PmfmVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableShort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author peck7 on 09/06/2020.
 */
@Slf4j
public class DenormalizedBatchRepositoryImpl
    extends SumarisJpaRepositoryImpl<DenormalizedBatch, Integer, DenormalizedBatchVO>
    implements DenormalizedBatchRepositoryExtend<DenormalizedBatchVO> {

    private final SumarisConfiguration config;
    private final PmfmRepository pmfmRepository;

    @Autowired
    @Lazy
    private DenormalizedBatchRepository self;

    @Autowired
    public DenormalizedBatchRepositoryImpl(EntityManager entityManager,
                                           SumarisConfiguration config,
                                           PmfmRepository pmfmRepository) {
        super(DenormalizedBatch.class, entityManager);
        this.config = config;
        this.pmfmRepository = pmfmRepository;
    }

    @Override
    public Class<DenormalizedBatchVO> getVOClass() {
        return DenormalizedBatchVO.class;
    }

    @Override
    public void toVO(DenormalizedBatch source, DenormalizedBatchVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        Integer operationId = source.getOperation() != null ? source.getOperation().getId() : null;
        if (copyIfNull || source.getOperation() != null) {
            target.setOperationId(operationId);
        }
    }

    @Override
    public void toEntity(DenormalizedBatchVO source, DenormalizedBatch target, boolean copyIfNull) {
        // Always same id
        target.setId(source.getId());

        super.toEntity(source, target, copyIfNull);

        /*if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
            } else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }*/

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
            } else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        // Parent operation
        Integer operationId = source.getOperationId() != null ? source.getOperationId() : (source.getOperation() != null ? source.getOperation().getId() : null);
        source.setOperationId(operationId);
        if (copyIfNull || (operationId != null)) {
            if (operationId == null) {
                target.setOperation(null);
            } else {
                target.setOperation(load(Operation.class, operationId));
            }
        }

        // Parent sale
        Integer saleId = source.getSaleId() != null ? source.getSaleId() : (source.getSale() != null ? source.getSale().getId() : null);
        source.setSaleId(saleId);
        if (copyIfNull || (saleId != null)) {
            if (saleId == null) {
                target.setSale(null);
            } else {
                target.setSale(load(Sale.class, saleId));
            }
        }

    }

    @Override
    public List<DenormalizedBatchVO> saveAllByOperationId(int operationId, @Nonnull List<DenormalizedBatchVO> sources) {

        // Set parent link
        sources.forEach(b -> b.setOperationId(operationId));

        // Get existing fishing areas
        Set<Integer> existingIds = self.getAllIdsByOperationId(operationId);

        // Save
        sources.forEach(b -> {
            save(b);
            existingIds.remove(b.getId());
        });

        // Delete remaining objects
        existingIds.forEach(this::deleteById);

        return sources;
    }

    @Override
    public List<DenormalizedBatchVO> saveAllBySaleId(int saleId, @Nonnull List<DenormalizedBatchVO> sources) {

        // Set parent link
        sources.forEach(b -> b.setSaleId(saleId));

        // Get existing fishing areas
        Set<Integer> existingIds = self.getAllIdsBySaleId(saleId);

        // Save
        sources.forEach(b -> {
            save(b);
            existingIds.remove(b.getId());
        });

        // Delete remaining objects
        existingIds.forEach(this::deleteById);

        return sources;
    }

    @Override
    public List<DenormalizedBatchVO> denormalized(BatchVO catchBatch) {

        final MutableShort flatRankOrder = new MutableShort(0);

        return TreeNodeEntities.<BatchVO, DenormalizedBatchVO>streamAllAndMap(catchBatch, (source, parent) -> {
            DenormalizedBatchVO target = toVO(source);

            // Add to parent
            if (parent != null) {
                target.setParent(parent);
                if (parent.getChildren() == null) {
                    parent.setChildren(Lists.newArrayList(target));
                } else {
                    parent.getChildren().add(target);
                }
            }

            // Depth level
            if (parent == null) {
                target.setTreeLevel((short)1); // First level
            }
            else {
                target.setTreeLevel((short)(parent.getTreeLevel() + 1));
                // Inherit taxon group
                if (target.getInheritedTaxonGroup() == null && parent.getInheritedTaxonGroup() != null) {
                    target.setInheritedTaxonGroup(parent.getInheritedTaxonGroup());
                }
                // Inherit taxon name
                if (target.getInheritedTaxonName() == null && parent.getInheritedTaxonName() != null) {
                    target.setInheritedTaxonName(parent.getInheritedTaxonName());
                }
                // Inherit location
                if (parent.getLocationId() != null) {
                    target.setLocationId(parent.getLocationId());
                }
                // Inherit quality flag (keep the worst value)
                if (parent.getQualityFlagId() > target.getQualityFlagId()) {
                    target.setQualityFlagId(parent.getQualityFlagId());
                }

                // If current quality is out of stats
                if (target.getQualityFlagId() >= QualityFlagEnum.OUT_STATS.getId()) {
                    // Force both parent and current parent exhaustive inventory to FALSE
                    parent.setExhaustiveInventory(Boolean.FALSE);
                    target.setExhaustiveInventory(Boolean.FALSE);
                }
            }

         return target;
        })
        // Sort
        .sorted(Comparator.comparing(this::computeFlatOrder))
        .map(target -> {
            // Flat rank order
            flatRankOrder.increment();
            target.setFlatRankOrder(flatRankOrder.getValue());

            if (target.getParent() == null) {
                computeTreeIndent(target);
            }
            log.warn(String.format("%s %s %s", target.getTreeIndent(), target.getLabel(), target.getRankOrder()));

            boolean isExhaustive = target.getInheritedTaxonName() != null || Boolean.TRUE.equals(target.getExhaustiveInventory());
            if (isExhaustive) {

            }
            return target;
        })
        .collect(Collectors.toList());
    }

    public DenormalizedBatch toEntity(DenormalizedBatchVO vo) {
        Preconditions.checkNotNull(vo);
        DenormalizedBatch entity = null;

        entity = createEntity();

        toEntity(vo, entity, true);

        return entity;
    }

    @Override
    public <S extends DenormalizedBatch> S save(S entity) {
        this.getSession().saveOrUpdate(entity);
        return entity;
    }

    /* -- protected methods -- */


    protected DenormalizedBatchVO toVO(BatchVO source) {
        DenormalizedBatchVO target = new DenormalizedBatchVO();
        toVO(source, target, true);
        return target;
    }

    protected void toVO(BatchVO source, DenormalizedBatchVO target, boolean copyIfNull) {
        Beans.copyProperties(source, target);

        // Quality flag (default value)
        if (target.getQualityFlagId() == null) {
            target.setQualityFlagId(QualityFlagEnum.NOT_QUALIFED.getId());
        }

        if (source.getMeasurementValues() != null) {
            Set<Integer> pmfmIds = source.getMeasurementValues().keySet();

            // Weight
            pmfmIds.stream().filter(this::isWeightPmfm)
                    .forEach(pmfmId -> {
                        String valStr = source.getMeasurementValues().get(pmfmId);
                        if (StringUtils.isNotBlank(valStr)) {
                            Double weight = Double.parseDouble(valStr);
                            target.setWeight(weight);
                            target.setWeightMethodId(getPmfmMethodId(pmfmId));
                        }
                    });

            // landing / discard
            pmfmIds.stream().filter(pmfmId -> pmfmId == PmfmEnum.DISCARD_OR_LANDING.getId())
                    .map(pmfmId -> source.getMeasurementValues().get(pmfmId))
                    .filter(Objects::nonNull)
                    .map(Integer::parseInt)
                    .findFirst()
                    .ifPresent(qvId -> {
                        target.setIsLanding(QualitativeValueEnum.LANDING.getId() == qvId);
                        target.setIsDiscard(QualitativeValueEnum.DISCARD.getId() == qvId);
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

            // landing / discard;
            if (source.getSortingMeasurements() != null) {
                source.getSortingMeasurements().stream()
                        .filter(m -> m.getPmfmId() == PmfmEnum.DISCARD_OR_LANDING.getId())
                        .map(MeasurementVO::getQualitativeValue)
                        .filter(Objects::nonNull)
                        .map(IValueObject::getId)
                        .findFirst()
                        .ifPresent(qvId -> {
                            target.setIsLanding(QualitativeValueEnum.LANDING.getId() == qvId);
                            target.setIsDiscard(QualitativeValueEnum.DISCARD.getId() == qvId);
                        });
            }
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
        return pmfmRepository.hasLabelPrefix(pmfmId, "WEIGHT");
    }

    protected Integer getPmfmMethodId(int pmfmId) {
        PmfmVO pmfm = pmfmRepository.get(pmfmId);
        return pmfm != null ? pmfm.getMethodId() : null;
    }

    protected Double computeIndirectWeight(DenormalizedBatchVO batch) {
        return 0d;
    }

    protected double computeFlatOrder(DenormalizedBatchVO b) {
        return (b.getParent() != null ? computeFlatOrder(b.getParent()) : 0d)
                + (b.getRankOrder() != null ? b.getRankOrder().doubleValue() : 1d) * Math.pow(10, -1 * (b.getTreeLevel() - 1 ) * 3);
    }

    protected void computeTreeIndent(DenormalizedBatchVO target) {
        computeTreeIndent(target, "", true);
    }

    protected void computeTreeIndent(DenormalizedBatchVO target, String inheritedTreeIndent, boolean isLast) {
        if (target.getParent() == null) {
            target.setTreeIndent("-");
        } else {
            target.setTreeIndent(inheritedTreeIndent + (isLast? " |_" : " |-"));
        }

        List<DenormalizedBatchVO> children = target.getChildren();
        if (CollectionUtils.isNotEmpty(children)) {
            final String newInheritedTreeIndent = inheritedTreeIndent + (isLast ? " " : "| ");
            for (int i = 0; i < children.size(); i++) {
                computeTreeIndent(children.get(i), newInheritedTreeIndent, i == children.size() - 1);
            }
        }
    }
}
