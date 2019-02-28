package net.sumaris.core.service.referential;

import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.model.referential.transcribing.Translate;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

public interface TranslationService {


    List<Translate> getTranslations();

    ReferentialVO translateReferential(ReferentialVO ref, String locale);

    void translateReferentials(List<ReferentialVO> refs, String locale);

    void reload();

}
