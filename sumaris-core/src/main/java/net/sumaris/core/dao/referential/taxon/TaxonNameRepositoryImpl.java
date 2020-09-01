package net.sumaris.core.dao.referential.taxon;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevelId;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author peck7 on 31/07/2020.
 */
public class TaxonNameRepositoryImpl
    extends ReferentialRepositoryImpl<TaxonName, TaxonNameVO, TaxonNameFilterVO, ReferentialFetchOptions>
    implements TaxonNameSpecifications {

    private static final Logger log =
        LoggerFactory.getLogger(ReferentialRepositoryImpl.class);

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
                .levelIds(new Integer[]{TaxonomicLevelId.SPECIES.getId(), TaxonomicLevelId.SUBSPECIES.getId()})
                .build(),
            Pageable.unpaged()
        );

    }

    @Override
    public TaxonNameVO getTaxonNameReferent(Integer referenceTaxonId) {

        List<TaxonNameVO> taxonNames = findByFilter(
            TaxonNameFilterVO.builder()
                .referenceTaxonId(referenceTaxonId)
                .withSynonyms(false)
                .build(),
            Pageable.unpaged()
        );
        if (CollectionUtils.isEmpty(taxonNames)) return null;
        if (taxonNames.size() > 1) {
            log.warn(String.format("ReferenceTaxon {id=%s} has more than one TaxonNames, with IS_REFERENT=1. Will use the first found.", referenceTaxonId));
        }
        return taxonNames.get(0);
    }

    @Override
    public List<TaxonNameVO> getAllByTaxonGroupId(Integer taxonGroupId) {

        return findByFilter(
            TaxonNameFilterVO.builder()
                .levelIds(new Integer[]{TaxonomicLevelId.SPECIES.getId(), TaxonomicLevelId.SUBSPECIES.getId()})
                .taxonGroupId(taxonGroupId)
                .build(),
            Pageable.unpaged()
        );
    }

    @Override
    protected Specification<TaxonName> toSpecification(TaxonNameFilterVO filter) {

        return super.toSpecification(filter)
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
}
