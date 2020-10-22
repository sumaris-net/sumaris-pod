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
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevelEnum;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
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

    public TaxonNameRepositoryImpl(EntityManager entityManager) {
        super(TaxonName.class, TaxonNameVO.class, entityManager);
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
    public Optional<TaxonNameVO> findTaxonNameReferent(Integer referenceTaxonId) {

        List<TaxonNameVO> taxonNames = findByFilter(
            TaxonNameFilterVO.builder()
                .referenceTaxonId(referenceTaxonId)
                .withSynonyms(false)
                .build(),
            Pageable.unpaged()
        );
        if (CollectionUtils.isEmpty(taxonNames)) return Optional.empty();
        if (taxonNames.size() > 1) {
            log.warn(String.format("ReferenceTaxon {id=%s} has more than one TaxonNames, with IS_REFERENT=1. Will use the first found.", referenceTaxonId));
        }
        return Optional.ofNullable(taxonNames.get(0));
    }

    @Override
    public List<TaxonNameVO> getAllByTaxonGroupId(Integer taxonGroupId) {

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
            .and(inLevelIds(TaxonName.Fields.TAXONOMIC_LEVEL, filter));
    }

    protected List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, Pageable pageable) {

        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(pageable);

        TypedQuery<TaxonName> query = getQuery(toSpecification(filter), pageable);

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
        target.setReferenceTaxonId(source.getReferenceTaxon().getId());

        // Taxonomic level id
        target.setTaxonomicLevelId(source.getTaxonomicLevel().getId());
    }
}
