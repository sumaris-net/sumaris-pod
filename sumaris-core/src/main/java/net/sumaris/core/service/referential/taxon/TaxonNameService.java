package net.sumaris.core.service.referential.taxon;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintStream;
import java.util.List;

@Transactional
public interface TaxonNameService {

    @Transactional(readOnly = true)
    List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

    @Transactional(readOnly = true)
    List<TaxonNameVO> getAll(boolean withSynonyms);

    @Transactional(readOnly = true)
    List<TaxonNameVO> getAllByTaxonGroup(Integer taxonGroupId);
}
