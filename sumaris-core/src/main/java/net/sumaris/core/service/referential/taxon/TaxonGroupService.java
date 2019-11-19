package net.sumaris.core.service.referential.taxon;


import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface TaxonGroupService {

    @Transactional
    void updateTaxonGroupHierarchies();

    List<TaxonGroupVO> findTargetSpeciesByFilter(ReferentialFilterVO filter,
                                                 int offset,
                                                 int size,
                                                 String sortAttribute,
                                                 SortDirection sortDirection);
}
