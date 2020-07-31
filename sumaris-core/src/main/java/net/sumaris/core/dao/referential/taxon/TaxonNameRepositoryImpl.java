package net.sumaris.core.dao.referential.taxon;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
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

    public TaxonNameRepositoryImpl(EntityManager entityManager) {
        super(TaxonName.class, entityManager);
    }

    @Override
    public List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {

        Preconditions.checkNotNull(filter);

        String searchText = Daos.getEscapedSearchText(filter.getSearchText());

        TypedQuery<TaxonName> query = getQuery(toSpecification(filter), TaxonName.class,
            Pageables.create(offset, size, sortAttribute, sortDirection));

        Parameter<String> searchTextParam = query.getParameter(ReferentialSpecifications.SEARCH_TEXT_PARAMETER, String.class);
        if (searchTextParam != null) {
            query.setParameter(searchTextParam, searchText);
        }

        return query.getResultStream()
            .distinct()
            .map(this::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public List<TaxonNameVO> getAll(boolean withSynonyms) {
        return null;
    }

    @Override
    public TaxonNameVO getTaxonNameReferent(Integer referenceTaxonId) {
        return null;
    }

    @Override
    public List<TaxonNameVO> getAllByTaxonGroupId(Integer taxonGroupId) {
        return null;
    }

    @Override
    public Specification<TaxonName> toSpecification(TaxonNameFilterVO filter) {
        Preconditions.checkNotNull(filter);

        return Specification
            .where(withTaxonGroupId(filter.getTaxonGroupId()))
            .and(withTaxonGroupIds(filter.getTaxonGroupIds()))
            .and(withSynonyms(filter.getWithSynonyms()))
            .and(searchOrJoinSearchText(filter))
            .and(inLevelIds(TaxonName.Fields.TAXONOMIC_LEVEL, filter.getLevelIds()))
            .and(inStatusIds(filter.getStatusIds()));
    }
}
