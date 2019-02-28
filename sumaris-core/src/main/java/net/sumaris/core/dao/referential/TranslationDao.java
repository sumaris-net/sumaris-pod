package net.sumaris.core.dao.referential;

import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.model.referential.transcribing.Translate;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;


public interface TranslationDao {

    public List<Translate> getTranslations();

    public Optional<Translate> getTranslation(String typeName, String objectId, String locale);

    public Optional<String> getTranslationOf(String typeName, String objectId, String locale);

    public void printAll();


}
