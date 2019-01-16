package net.sumaris.core.service.referential.taxon;

import net.sumaris.core.dao.referential.taxon.TaxonNameDao;
import net.sumaris.core.service.referential.ReferentialServiceImpl;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("taxonNameService")
public class TaxonNameServiceImpl implements TaxonNameService {

    private static final Log log = LogFactory.getLog(ReferentialServiceImpl.class);

    @Autowired
    protected TaxonNameDao taxonNameDao;

    @Override
    public List<TaxonNameVO> getAll(boolean withSynonyms) {
        return taxonNameDao.getAll(withSynonyms);
    }
}
