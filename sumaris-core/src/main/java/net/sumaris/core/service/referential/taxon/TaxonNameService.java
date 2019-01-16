package net.sumaris.core.service.referential.taxon;

import net.sumaris.core.vo.referential.TaxonNameVO;

import java.io.PrintStream;
import java.util.List;

public interface TaxonNameService {

    List<TaxonNameVO> getAll(boolean withSynonyms);
}
