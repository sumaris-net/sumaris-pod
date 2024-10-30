package net.sumaris.core.dao.administration.programStrategy;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.pmfm.PmfmVO;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.jpa.QueryHints;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class PmfmStrategyRepositoryImpl
    extends SumarisJpaRepositoryImpl<PmfmStrategy, Integer, PmfmStrategyVO>
        implements PmfmStrategyRepository {


    private final ReferentialDao referentialDao;
    private final PmfmRepository pmfmRepository;


    PmfmStrategyRepositoryImpl(EntityManager entityManager,
                               ReferentialDao referentialDao,
                               PmfmRepository pmfmRepository) {
        super(PmfmStrategy.class, PmfmStrategyVO.class, entityManager);
        this.referentialDao = referentialDao;
        this.pmfmRepository = pmfmRepository;
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.PMFM_STRATEGIES_BY_FILTER)
    public List<PmfmStrategyVO> findByFilter(PmfmStrategyFilterVO filter, PmfmStrategyFetchOptions fetchOptions) {
        Specification<PmfmStrategy> spec = filter != null ? toSpecification(filter, fetchOptions) : null;
        TypedQuery<PmfmStrategy> query = getQuery(spec, getDomainClass(), Sort.by(PmfmStrategy.Fields.STRATEGY, PmfmStrategy.Fields.ACQUISITION_LEVEL, PmfmStrategy.Fields.RANK_ORDER));

        // Add hints
        configureQuery(query, fetchOptions);

        try (Stream<PmfmStrategy> stream = streamQuery(query)) {
            return stream.map(entity -> toVO(entity, fetchOptions)).toList();
        }
    }

    @Override
    public PmfmStrategyVO toVO(PmfmStrategy source) {
        return toVO(source, PmfmStrategyFetchOptions.DEFAULT);
    }

    @Override
    public PmfmStrategyVO toVO(PmfmStrategy source, PmfmStrategyFetchOptions fetchOptions) {
        if (source == null) return null;
        return toVO(source, source.getPmfm(), fetchOptions);
    }

    @Override
    public PmfmStrategyVO toVO(PmfmStrategy source, Pmfm pmfm, PmfmStrategyFetchOptions fetchOptions) {
        if (source == null) return null;
        fetchOptions = PmfmStrategyFetchOptions.nullToDefault(fetchOptions);

        PmfmStrategyVO target = new PmfmStrategyVO();

        Beans.copyProperties(source, target);

        // Strategy Id
        target.setStrategyId(source.getStrategy().getId());

        // Pmfm
        if (pmfm != null) {
            target.setPmfmId(pmfm.getId());

            // Fetch pmfm
            if (fetchOptions.isWithPmfms()) {
                PmfmVO targetPmfm = pmfmRepository.get(pmfm.getId());
                target.setPmfm(targetPmfm);
            }
        }
        else {
            if (source.getParameter() != null) target.setParameterId(source.getParameter().getId());
            if (source.getMatrix() != null) target.setMatrixId(source.getMatrix().getId());
            if (source.getFraction() != null) target.setFractionId(source.getFraction().getId());
            if (source.getMethod() != null) target.setMethodId(source.getMethod().getId());
        }

        // Acquisition Level
        if (source.getAcquisitionLevel() != null) {
            target.setAcquisitionLevel(referentialDao.getAcquisitionLevelLabelById(source.getAcquisitionLevel().getId()));
        }

        // Gears
        if (CollectionUtils.isNotEmpty(source.getGears())) {
            List<Integer> gearIds = Beans.collectIds(source.getGears());
            target.setGearIds(gearIds);

            // Fetch gear's labels
            if (fetchOptions.isWithGears()) {
                List<String> gears = referentialDao.findLabelsByFilter(Gear.class.getSimpleName(),
                    ReferentialFilterVO.builder()
                    .includedIds(gearIds.toArray(new Integer[0]))
                    .build());
                target.setGears(gears);
            }
        }

        // Taxon groups
        if (CollectionUtils.isNotEmpty(source.getTaxonGroups())) {
            target.setTaxonGroupIds(Beans.collectIds(source.getTaxonGroups()));
        }

        // Reference taxons
        if (CollectionUtils.isNotEmpty(source.getReferenceTaxons())) {
            target.setReferenceTaxonIds(Beans.collectIds(source.getReferenceTaxons()));
        }

        return target;
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheConfiguration.Names.PMFM_STRATEGIES_BY_FILTER, allEntries = true),
            @CacheEvict(cacheNames = CacheConfiguration.Names.DENORMALIZED_PMFM_BY_FILTER, allEntries = true)
        }
    )
    public List<PmfmStrategyVO> saveByStrategyId(int strategyId, @Nonnull List<PmfmStrategyVO> sources) {
        Preconditions.checkNotNull(sources);

        Strategy parent = getById(Strategy.class, strategyId);

        // Fill strategy id
        sources.forEach(source -> source.setStrategyId(strategyId));

        // Load existing entities
        Map<Integer, PmfmStrategy> existingEntities = Beans.splitById(parent.getPmfms());

        sources.forEach(source -> {
            save(source);
            existingEntities.remove(source.getId());
        });

        // Delete remaining
        existingEntities.values().forEach(ps -> {
            this.delete(ps);
            parent.getPmfms().remove(ps);
        });

        return sources.isEmpty() ? null : sources;
    }

    @Override
    public void toEntity(PmfmStrategyVO source, PmfmStrategy target, boolean copyIfNull) {
        Beans.copyProperties(source, target);

        // Parent
        if (source.getStrategyId() != null) {
            target.setStrategy(getReference(Strategy.class, source.getStrategyId()));
        }

        // Pmfm, Parameter, Matrix, Fraction, Method
        Integer pmfmId = source.getPmfmId() != null ? source.getPmfmId()
                : (source.getPmfm() != null ? source.getPmfm().getId() : null);
        Integer parameterId = source.getParameterId() != null ? source.getParameterId()
                : (source.getParameter() != null ? source.getParameter().getId() : null);
        Integer matrixId = source.getMatrixId() != null ? source.getMatrixId()
                : (source.getMatrix() != null ? source.getMatrix().getId() : null);
        Integer fractionId = source.getFractionId() != null ? source.getFractionId()
                : (source.getFraction() != null ? source.getFraction().getId() : null);
        Integer methodId = source.getMethodId() != null ? source.getMethodId()
                : (source.getMethod() != null ? source.getMethod().getId() : null);

        boolean hasPmfmPart = (parameterId != null || matrixId != null || fractionId != null || methodId != null);
        if (pmfmId == null && !hasPmfmPart) {
            throw new DataIntegrityViolationException("Invalid PmfmStrategy: missing a Pmfm or a part of Pmfm (Parameter, Matrix, Fraction or Method)");
        }
        if (pmfmId != null && hasPmfmPart) {
            throw new DataIntegrityViolationException("Invalid PmfmStrategy: Cannot have both a Pmfm AND a part of Pmfm (Parameter, Matrix, Fraction or Method)");
        }

        if (copyIfNull || pmfmId != null) {
            if (pmfmId != null) {
                target.setPmfm(getReference(Pmfm.class, pmfmId));
            }
            else {
                target.setPmfm(null);
            }
        }
        if (copyIfNull || parameterId != null) {
            if (parameterId != null) {
                target.setParameter(getReference(Parameter.class, parameterId));
            }
            else {
                target.setParameter(null);
            }
        }
        if (copyIfNull || matrixId != null) {
            if (matrixId != null) {
                target.setMatrix(getReference(Matrix.class, matrixId));
            }
            else {
                target.setMatrix(null);
            }
        }
        if (copyIfNull || fractionId != null) {
            if (fractionId != null) {
                target.setFraction(getReference(Fraction.class, fractionId));
            }
            else {
                target.setFraction(null);
            }
        }
        if (copyIfNull || methodId != null) {
            if (methodId != null) {
                target.setMethod(getReference(Method.class, methodId));
            }
            else {
                target.setMethod(null);
            }
        }

        // Acquisition Level
        String acquisitionLevel = source.getAcquisitionLevel();
        if (copyIfNull || acquisitionLevel != null) {
            if (acquisitionLevel != null) {
                target.setAcquisitionLevel(getReference(AcquisitionLevel.class, referentialDao.getAcquisitionLevelIdByLabel(acquisitionLevel)));
            }
            else {
                target.setAcquisitionLevel(null);
            }
        }

        // Gears
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getGearIds())) {
            target.getGears().clear();
            if (CollectionUtils.isNotEmpty(source.getGearIds())) {
                target.getGears().addAll(loadAllAsSet(Gear.class, source.getGearIds(), true));
            }
        }

        // Taxon Groups
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getTaxonGroupIds())) {
            target.getTaxonGroups().clear();
            if (CollectionUtils.isNotEmpty(source.getTaxonGroupIds())) {
                target.getTaxonGroups().addAll(loadAllAsSet(TaxonGroup.class, source.getTaxonGroupIds(), true));
            }
        }

        // Taxon Names
        if (copyIfNull || CollectionUtils.isNotEmpty(source.getReferenceTaxonIds())) {
            target.getReferenceTaxons().clear();
            if (CollectionUtils.isNotEmpty(source.getReferenceTaxonIds())) {
                target.getReferenceTaxons().addAll(loadAllAsSet(ReferenceTaxon.class, source.getReferenceTaxonIds(), true));
            }
        }
    }

    protected void configureQuery(TypedQuery<PmfmStrategy> query, @Nullable PmfmStrategyFetchOptions fetchOptions) {
        if (fetchOptions != null && fetchOptions.isWithPmfms()) {
            // Prepare load graph
            EntityManager em = getEntityManager();
            EntityGraph<?> entityGraph = em.getEntityGraph(PmfmStrategy.GRAPH_PMFM);

            Subgraph<Pmfm> pmfmSubGraph = entityGraph.addSubgraph(PmfmStrategy.Fields.PMFM);
            pmfmSubGraph.addSubgraph(Pmfm.Fields.PARAMETER);
            pmfmSubGraph.addSubgraph(Pmfm.Fields.MATRIX);
            pmfmSubGraph.addSubgraph(Pmfm.Fields.FRACTION);
            pmfmSubGraph.addSubgraph(Pmfm.Fields.METHOD);
            pmfmSubGraph.addSubgraph(Pmfm.Fields.UNIT);

            query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
        }
    }
}
