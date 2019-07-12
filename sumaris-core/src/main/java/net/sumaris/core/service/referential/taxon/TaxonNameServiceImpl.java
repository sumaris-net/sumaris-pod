package net.sumaris.core.service.referential.taxon;

import net.sumaris.core.dao.referential.taxon.TaxonNameDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("taxonNameService")
public class TaxonNameServiceImpl implements TaxonNameService {

    private static final Logger log = LoggerFactory.getLogger(TaxonNameServiceImpl.class);

    @Autowired
    protected TaxonNameDao taxonNameDao;

    @Override
    public List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        return taxonNameDao.findByFilter(filter, offset, size, sortAttribute, sortDirection);
    }

    @Override
    public List<TaxonNameVO> getAll(boolean withSynonyms) {
        return taxonNameDao.getAll(withSynonyms);
    }

    @Override
    public List<TaxonNameVO> getAllByTaxonGroup(Integer taxonGroupId) {
        return taxonNameDao.getAllByTaxonGroupId(taxonGroupId);
    }
}
