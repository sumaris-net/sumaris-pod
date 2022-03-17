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
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.core.model.referential.taxon.TaxonomicLevelEnum;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.referential.TaxonNameFetchOptions;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * @author peck7 on 31/07/2020.
 */
@Slf4j
public class TaxonNameRepositoryImpl
        extends ReferentialRepositoryImpl<TaxonName, TaxonNameVO, TaxonNameFilterVO, TaxonNameFetchOptions>
        implements TaxonNameSpecifications {

    @Autowired
    private TaxonomicLevelRepository taxonomicLevelRepository;

    public TaxonNameRepositoryImpl(EntityManager entityManager) {
        super(TaxonName.class, TaxonNameVO.class, entityManager);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_ID, key = "#id")
    public TaxonNameVO get(int id) {
        return super.get(id);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_TAXON_REFERENCE_ID, unless = "#result == null")
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
        List<TaxonNameVO> taxonNames = findAll(
                TaxonNameFilterVO.builder()
                        .referenceTaxonId(referenceTaxonId)
                        .withSynonyms(false)
                        .build()
        );
        return taxonNames;
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.TAXON_NAMES_BY_TAXON_GROUP_ID, unless = "#result == null")
    public List<TaxonNameVO> getAllByTaxonGroupId(int taxonGroupId) {
        return findAll(
                TaxonNameFilterVO.builder()
                        .levelIds(new Integer[]{TaxonomicLevelEnum.SPECIES.getId(), TaxonomicLevelEnum.SUBSPECIES.getId()})
                        .taxonGroupId(taxonGroupId)
                        .withSynonyms(false)
                        .build()
        );
    }

    @Override
    protected Specification<TaxonName> toSpecification(TaxonNameFilterVO filter, TaxonNameFetchOptions fetchOptions) {
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
    @Cacheable(cacheNames = CacheConfiguration.Names.REFERENCE_TAXON_ID_BY_TAXON_NAME_ID)
    public Integer getReferenceTaxonIdById(int id) {
        return getEntityManager()
                .createNamedQuery("TaxonName.referenceTaxonIdById", Integer.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    public Long countByFilter(TaxonNameFilterVO filter) {
        Preconditions.checkNotNull(filter);

        return getCountQuery(toSpecification(filter), TaxonName.class).getSingleResult();
    }

    @Override
    protected void toVO(TaxonName source, TaxonNameVO target, TaxonNameFetchOptions fetchOptions, boolean copyIfNull) {
        fetchOptions = TaxonNameFetchOptions.nullToEmpty(fetchOptions);

        super.toVO(source, target, fetchOptions, copyIfNull);

        // Convert boolean -> Boolean
        target.setIsReferent(source.isReferent());
        target.setIsNaming(source.isNaming());
        target.setIsVirtual(source.isVirtual());

        // Reference taxon id
        if (source.getReferenceTaxon() != null) {
            target.setReferenceTaxonId(source.getReferenceTaxon().getId());
        }

        // Taxonomic level
        if (source.getTaxonomicLevel() != null) {
            target.setTaxonomicLevelId(source.getTaxonomicLevel().getId());
            if (fetchOptions.isWithTaxonomicLevel()) {
                // Get parent using get(), to be able to use cache
                ReferentialVO taxonomicLevel = this.taxonomicLevelRepository.get(target.getTaxonomicLevelId());
                target.setTaxonomicLevel(taxonomicLevel);
            }
        }

        // Parent taxon name
        if (source.getParent() != null) {
            target.setParentId(source.getParent().getId());

            // Get parent, only if need
            if (fetchOptions.isWithParentTaxonName()) {
                TaxonNameVO parent = this.get(target.getParentId());
                target.setParentTaxonName(parent);
            }
        }
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_ID, key = "#source.id", condition = "#source.id != null"),
                    @CacheEvict(cacheNames = CacheConfiguration.Names.TAXON_NAME_BY_FILTER, allEntries = true)
            }
    )
    public TaxonNameVO save(TaxonNameVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getReferenceTaxonId(), "Missing 'ReferenceTaxonId'");
        Preconditions.checkNotNull(source.getTaxonomicLevelId(), "Missing 'TaxonomicLevelId'");
        Preconditions.checkNotNull(source.getName(), "Missing 'name'");
        Preconditions.checkNotNull(source.getStatusId(), "Missing 'statusId'");
        Preconditions.checkNotNull(source.getIsNaming(), "Missing 'IsNaming'");
        Preconditions.checkNotNull(source.getIsReferent(), "Missing 'IsReferent'");
        Preconditions.checkNotNull(source.getIsVirtual(), "Missing 'IsVirtual'");

        if (source.getId() == null && source.getStatusId() == null)
            // Set default status to Temporary
            source.setStatusId(StatusEnum.TEMPORARY.getId());

        return super.save(source);
    }
}
