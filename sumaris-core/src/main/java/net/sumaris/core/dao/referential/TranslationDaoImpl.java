package net.sumaris.core.dao.referential;

import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.transcribing.Translate;
import net.sumaris.core.model.referential.transcribing.TranscribingItem;
import net.sumaris.core.model.referential.transcribing.TranscribingItemType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository("translationDao")
public class TranslationDaoImpl extends HibernateDaoSupport implements TranslationDao {

    private static final Logger log = LoggerFactory.getLogger(TranslationDaoImpl.class);


    @Cacheable(cacheNames = CacheNames.TRANSLATIONS)
    public List<Translate> getTranslations() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery query = builder.createQuery(Translate.class);

        Root<TranscribingItem> transcribe = query.from(TranscribingItem.class);
        Join<TranscribingItem, TranscribingItemType> typeJoin = transcribe.join(TranscribingItem.PROPERTY_TYPE, JoinType.INNER);

        query.multiselect(
                transcribe.get(TranscribingItem.PROPERTY_OBJECT_ID),
                transcribe.get(TranscribingItem.PROPERTY_EXTERNAL_CODE),
                typeJoin.get(TranscribingItemType.PROPERTY_LABEL),
                typeJoin.get(TranscribingItemType.PROPERTY_ID)
        );


        // query.where(predicates.stream().toArray(Predicate[]::new));

        TypedQuery<Translate> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.TRANSLATIONS_BY_ID, key = "#typeName + '_' + #objectId")
    public Optional<Translate> getTranslation(String typeName, String objectId, String locale) {
        String loc = locale.substring(0, 2).toUpperCase();
        //log.info("Searching translation for " + table + " " + loc + " item " + id);
        return getTranslations().stream()
                .filter(t -> t.getObjectId().equals(objectId))
                .filter(t -> t.getTypeName().equals(typeName + "." + loc) ||
                        t.getTypeName().equals(typeName + "_" + loc))
                //.peek(x -> log.info("Found translation for " + x.toString()))
                .findAny()
                ;
    }

    @Cacheable(cacheNames = CacheNames.TRANSLATIONS_BY_ID, key = "#typeName + '_' + #objectId")
    public Optional<String> getTranslationOf(String typeName, String objectId, String locale) {
        log.info("all translations:\n" + getTranslations().stream()
                .map(Translate::toString)
                .collect(Collectors.joining("\n")));
        return getTranslation(typeName, objectId, locale).map(Translate::getExternalCode);
    }

    public void printAll() {
        log.info("all translations:\n" + getTranslations().stream()
                .map(Translate::toString)
                .collect(Collectors.joining("\n")));
    }


    private void voiid() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        List<Integer> xParam2 = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
        CriteriaQuery<TranscribingItem> itemsQ = builder.createQuery(TranscribingItem.class);
        Root<TranscribingItem> rootIT = itemsQ.from(TranscribingItem.class);
        Expression xp = rootIT.get(TranscribingItem.PROPERTY_TYPE).get(TranscribingItemType.PROPERTY_ID).in(xParam2);

        CriteriaQuery<TranscribingItem> joined = itemsQ.select(rootIT).where(xp);


        log.info("test my request " + getEntityManager()
                .createQuery(joined)
                .getResultStream()
                .map(ti -> ti.getId() + " " + ti.getObjectId() + " " + ti.getExternalCode())
                .collect(Collectors.joining("\n")));


    }
}
