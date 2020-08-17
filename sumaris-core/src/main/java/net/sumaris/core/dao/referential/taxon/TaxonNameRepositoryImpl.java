package net.sumaris.core.dao.referential.taxon;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevelId;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author peck7 on 31/07/2020.
 */
public class TaxonNameRepositoryImpl
    extends ReferentialRepositoryImpl<TaxonName, TaxonNameVO, TaxonNameFilterVO>
    implements TaxonNameRepositoryExtend {

    private static final Logger log =
        LoggerFactory.getLogger(ReferentialRepositoryImpl.class);

    public TaxonNameRepositoryImpl(EntityManager entityManager) {
        super(TaxonName.class, entityManager);
    }

    @Override
    public List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {

        return findByFilter(filter, Pageables.create(offset, size, sortAttribute, sortDirection));
    }

    @Override
    public List<TaxonNameVO> getAll(boolean withSynonyms) {

        return findByFilter(
            TaxonNameFilterVO.taxonNameBuilder()
                .withSynonyms(withSynonyms)
                .levelIds(new Integer[]{TaxonomicLevelId.SPECIES.getId(), TaxonomicLevelId.SUBSPECIES.getId()})
                .build(),
            Pageable.unpaged()
        );

    }

    @Override
    public TaxonNameVO getTaxonNameReferent(Integer referenceTaxonId) {

        List<TaxonNameVO> taxonNames = findByFilter(
            TaxonNameFilterVO.taxonNameBuilder()
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
            TaxonNameFilterVO.taxonNameBuilder()
                .levelIds(new Integer[]{TaxonomicLevelId.SPECIES.getId(), TaxonomicLevelId.SUBSPECIES.getId()})
                .taxonGroupId(taxonGroupId)
                .build(),
            Pageable.unpaged()
        );
    }

    @Override
    public Specification<TaxonName> toSpecification(TaxonNameFilterVO filter) {
        Preconditions.checkNotNull(filter);

        return Specification
            .where(withTaxonGroupId(filter.getTaxonGroupId()))
            .and(withTaxonGroupIds(filter.getTaxonGroupIds()))
            .and(withSynonyms(filter.getWithSynonyms()))
            .and(withReferenceTaxonId(filter.getReferenceTaxonId()))
            .and(searchOrJoinSearchText(filter))
            .and(inLevelIds(TaxonName.Fields.TAXONOMIC_LEVEL, filter.getLevelIds()))
            .and(inStatusIds(filter.getStatusIds()));
    }

    @Override
    public Class<TaxonNameVO> getVOClass() {
        return TaxonNameVO.class;
    }

    protected List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, Pageable pageable) {

        Preconditions.checkNotNull(filter);

        String searchText = Daos.getEscapedSearchText(filter.getSearchText());

        TypedQuery<TaxonName> query = getQuery(toSpecification(filter), TaxonName.class, pageable);

        Parameter<String> searchTextParam = query.getParameter(ReferentialSpecifications.SEARCH_TEXT_PARAMETER, String.class);
        if (searchTextParam != null) {
            query.setParameter(searchTextParam, searchText);
        }

        return query.getResultStream()
            .distinct()
            .map(this::toVO)
            .collect(Collectors.toList());
    }
}
