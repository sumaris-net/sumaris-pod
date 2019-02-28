package net.sumaris.core.service.referential;

import net.sumaris.core.dao.referential.TranslationDao;
import net.sumaris.core.dao.referential.TranslationDaoImpl;
import net.sumaris.core.model.referential.transcribing.Translate;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service("translationService")
public class TranslationServiceImpl implements TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);


    @Autowired
    TranslationDao translationDAO;


    @Override
    public List<Translate> getTranslations() {
        return translationDAO.getTranslations();
    }


    public void reload(){
        translationDAO.printAll();
    }

    @Override
    public ReferentialVO translateReferential(ReferentialVO ref, String locale) {
        translationDAO.getTranslationOf("QUALITATIVE_VALUE", String.valueOf(ref.getId()), locale)
                .ifPresent(ref::setName);
        return ref;
    }

    @Override
    public void translateReferentials(List<ReferentialVO> refs, String locale) {
        if (refs != null)
            refs.forEach(qv ->
                    translationDAO.getTranslationOf("QUALITATIVE_VALUE", String.valueOf(qv.getId()), locale)
                            .ifPresent(qv::setName)
            );

    }

}
