package net.sumaris.core.dao.referential.taxon;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.technical.optimization.taxon.TaxonGroup2TaxonHierarchy;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import java.util.List;

/**
 * @author peck7 on 31/07/2020.
 */
public interface TaxonNameSpecifications extends ReferentialSpecifications<TaxonName> {

    // TODO use BindableSpecification

    default Specification<TaxonName> withReferenceTaxonId(Integer referentTaxonId) {
        if (referentTaxonId == null) return null;
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(TaxonName.Fields.REFERENCE_TAXON).get(ReferenceTaxon.Fields.ID), referentTaxonId);
    }

    default Specification<TaxonName> withSynonyms(Boolean withSynonyms) {
        if (Boolean.TRUE.equals(withSynonyms)) return null;
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(TaxonName.Fields.IS_REFERENT), Boolean.TRUE);
    }

    default Specification<TaxonName> withTaxonGroupId(Integer taxonGroupId) {
        if (taxonGroupId == null) return null;
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
            root.join(TaxonName.Fields.REFERENCE_TAXON, JoinType.INNER)
                .joinList(ReferenceTaxon.Fields.PARENT_TAXON_GROUPS, JoinType.INNER)
                .get(TaxonGroup2TaxonHierarchy.Fields.PARENT_TAXON_GROUP)
                .get(TaxonGroup.Fields.ID),
            taxonGroupId
        );
    }

    default Specification<TaxonName> withTaxonGroupIds(Integer[] taxonGroupIds) {
        if (ArrayUtils.isEmpty(taxonGroupIds)) return null;
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(
            root.join(TaxonName.Fields.REFERENCE_TAXON, JoinType.INNER)
                .joinList(ReferenceTaxon.Fields.PARENT_TAXON_GROUPS, JoinType.INNER)
                .get(TaxonGroup2TaxonHierarchy.Fields.PARENT_TAXON_GROUP)
                .get(TaxonGroup.Fields.ID)
        ).value(ImmutableList.copyOf(taxonGroupIds));
    }

    List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

    List<TaxonNameVO> getAll(boolean withSynonyms);

    @Cacheable(cacheNames = CacheNames.TAXON_NAME_BY_TAXON_REFERENCE_ID, unless = "#result == null")
    TaxonNameVO getTaxonNameReferent(Integer referenceTaxonId);

    @Cacheable(cacheNames = CacheNames.TAXON_NAMES_BY_TAXON_GROUP_ID, unless = "#result == null")
    List<TaxonNameVO> getAllByTaxonGroupId(Integer taxonGroupId);
}
