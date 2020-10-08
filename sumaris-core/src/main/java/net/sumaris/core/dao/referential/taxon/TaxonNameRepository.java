package net.sumaris.core.dao.referential.taxon;

import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.TaxonNameVO;

import java.util.Collection;
import java.util.List;

/**
 * @author peck7 on 31/07/2020.
 */
public interface TaxonNameRepository extends
    ReferentialRepository<TaxonName, TaxonNameVO, TaxonNameFilterVO, ReferentialFetchOptions>,
    TaxonNameSpecifications {

    List<TaxonName> getAllTaxonNameByParentTaxonNameIdInAndIsReferentTrue(Collection<Integer> parentIds);


}
