package net.sumaris.core.dao.referential.taxon;

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.core.model.referential.taxon.TaxonomicLevelEnum;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author peck7 on 31/07/2020.
 */
@Slf4j
public class TaxonNameRepositoryImpl
        extends ReferentialRepositoryImpl<TaxonName, TaxonNameVO, TaxonNameFilterVO, ReferentialFetchOptions>
        implements TaxonNameSpecifications {

    @Autowired
    private ReferentialDao referentialDao;

    public TaxonNameRepositoryImpl(EntityManager entityManager) {
        super(TaxonName.class, TaxonNameVO.class, entityManager);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_ID, key = "#id")
    public TaxonNameVO get(int id) {
        return super.get(id);
    }

    @Override
    public List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        return findByFilter(filter, Pageables.create(offset, size, sortAttribute, sortDirection));
    }

    @Override
    public List<TaxonNameVO> getAll(boolean withSynonyms) {

        return findByFilter(
                TaxonNameFilterVO.builder()
                        .withSynonyms(withSynonyms)
                        .levelIds(new Integer[]{TaxonomicLevelEnum.SPECIES.getId(), TaxonomicLevelEnum.SUBSPECIES.getId()})
                        .build(),
                Pageable.unpaged()
        );
    }

    @Override
    public Optional<TaxonNameVO> findReferentByReferenceTaxonId(int referenceTaxonId) {

        List<TaxonNameVO> taxonNames = findAllReferentByReferenceTaxonId(referenceTaxonId);

        if (CollectionUtils.isEmpty(taxonNames)) return Optional.empty();
        if (taxonNames.size() > 1) {
            log.warn(String.format("ReferenceTaxon {id=%s} has more than one TaxonNames, with IS_REFERENT=1. Will use the first found.", referenceTaxonId));
        }
        return Optional.ofNullable(taxonNames.get(0));
    }

    @Override
    public List<TaxonNameVO> findAllReferentByReferenceTaxonId(int referenceTaxonId) {

        List<TaxonNameVO> taxonNames = findByFilter(
                TaxonNameFilterVO.builder()
                        .referenceTaxonId(referenceTaxonId)
                        .withSynonyms(false)
                        .build(),
                Pageable.unpaged()
        );
        return taxonNames;
    }


    @Override
    public List<TaxonNameVO> getAllByTaxonGroupId(int taxonGroupId) {

        return findByFilter(
                TaxonNameFilterVO.builder()
                        .levelIds(new Integer[]{TaxonomicLevelEnum.SPECIES.getId(), TaxonomicLevelEnum.SUBSPECIES.getId()})
                        .taxonGroupId(taxonGroupId)
                        .withSynonyms(false)
                        .build(),
                Pageable.unpaged()
        );
    }

    @Override
    protected Specification<TaxonName> toSpecification(TaxonNameFilterVO filter, ReferentialFetchOptions fetchOptions) {

        return super.toSpecification(filter, fetchOptions)
                .and(withTaxonGroupId(filter.getTaxonGroupId()))
                .and(withTaxonGroupIds(filter.getTaxonGroupIds()))
                .and(withSynonyms(filter.getWithSynonyms()))
                .and(withReferenceTaxonId(filter.getReferenceTaxonId()))
                .and(inLevelIds(TaxonName.class, filter.getLevelIds()));
    }

    @Override
    public void toEntity(TaxonNameVO source, TaxonName target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        target.setReferent(source.getIsReferent());
        target.setVirtual(source.getIsVirtual());
        target.setNaming(source.getIsNaming());

        // Link to other entities
        Daos.setEntityProperties(getEntityManager(), target,
                TaxonName.Fields.PARENT, TaxonName.class, source.getParentId(),
                TaxonName.Fields.TAXONOMIC_LEVEL, TaxonomicLevel.class, source.getTaxonomicLevelId(),
                TaxonName.Fields.REFERENCE_TAXON, ReferenceTaxon.class, source.getReferenceTaxonId());
    }

    @Override
    public TaxonNameVO toVO(TaxonName source) {
        TaxonNameVO target = super.toVO(source);

        if (source.getReferenceTaxon() != null) {
            target.setReferenceTaxonId(source.getReferenceTaxon().getId());
        }

        if (source.getParent() != null) {
            target.setParentTaxonName(this.toVO(source.getParent()));
        }

        if (source.getTaxonomicLevel() != null) {
            target.setTaxonomicLevel(referentialDao.toVO(source.getTaxonomicLevel()));
        }

        return target;
    }

    protected List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, Pageable pageable) {

        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(pageable);

        TypedQuery<TaxonName> query = getQuery(toSpecification(filter), pageable);

        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
        }

        return query.getResultStream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public Integer getReferenceTaxonIdById(int id) {
        return getEntityManager()
                .createNamedQuery("TaxonName.referenceTaxonIdById", Integer.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    @Override
    protected void toVO(TaxonName source, TaxonNameVO target, ReferentialFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Convert boolean -> Boolean
        target.setIsReferent(source.isReferent());
        target.setIsNaming(source.isNaming());
        target.setIsVirtual(source.isVirtual());

        // Reference taxon id
        if (source.getReferenceTaxon() != null) {
            target.setReferenceTaxonId(source.getReferenceTaxon().getId());
        }

        // Taxonomic level id
        if (source.getTaxonomicLevel() != null) {
            target.setTaxonomicLevel(referentialDao.toVO(source.getTaxonomicLevel()));
            target.setTaxonomicLevelId(source.getTaxonomicLevel().getId());
        }

        if (source.getParent() != null) {
            target.setParentTaxonName(this.toVO(source.getParent()));
            target.setParentId(source.getParent().getId());
        }
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_ID, key = "#vo.id", condition = "#vo.id != null"),
                    @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_FILTER, allEntries = true)
            }
    )
    public TaxonNameVO save(TaxonNameVO vo) {
        Preconditions.checkNotNull(vo);
        Preconditions.checkNotNull(vo.getReferenceTaxonId(), "Missing 'ReferenceTaxonId'");
        Preconditions.checkNotNull(vo.getTaxonomicLevelId(), "Missing 'TaxonomicLevelId'");
        Preconditions.checkNotNull(vo.getName(), "Missing 'name'");
        Preconditions.checkNotNull(vo.getStatusId(), "Missing 'statusId'");
        Preconditions.checkNotNull(vo.getIsNaming(), "Missing 'IsNaming'");
        Preconditions.checkNotNull(vo.getIsReferent(), "Missing 'IsReferent'");
        Preconditions.checkNotNull(vo.getIsVirtual(), "Missing 'IsVirtual'");

        if (vo.getId() == null && vo.getStatusId() == null)
            // Set default status to Temporary
            vo.setStatusId(StatusEnum.TEMPORARY.getId());

        TaxonNameVO savedVo = super.save(vo);
        return savedVo;
    }
}
