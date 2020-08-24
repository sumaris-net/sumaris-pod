package net.sumaris.core.dao.referential;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author peck7 on 24/08/2020.
 */
public interface BaseRefRepository {

    interface QueryVisitor<R, T> {
        Expression<Boolean> apply(CriteriaQuery<R> query, Root<T> root);
    }

    ReferentialVO get(String entityName, int id);

    ReferentialVO get(Class<? extends IReferentialEntity> entityClass, int id);

    Date getLastUpdateDate();

    Date getLastUpdateDate(Collection<String> entityNames);

    Date maxUpdateDate(String entityName);

    List<ReferentialTypeVO> getAllTypes();

    List<ReferentialVO> getAllLevels(String entityName);

    ReferentialVO getLevelById(String entityName, int levelId);

    <T extends IReferentialEntity> Stream<T> streamByFilter(final Class<T> entityClass,
                                                            ReferentialFilterVO filter,
                                                            int offset,
                                                            int size,
                                                            String sortAttribute,
                                                            SortDirection sortDirection);

    List<ReferentialVO> findByFilter(String entityName,
                                     ReferentialFilterVO filter,
                                     int offset,
                                     int size,
                                     String sortAttribute,
                                     SortDirection sortDirection);

    Long countByFilter(final String entityName, ReferentialFilterVO filter);

    ReferentialVO findByUniqueLabel(String entityName, String label);

    <T extends IReferentialEntity> ReferentialVO toReferentialVO(T source);

    <T extends IReferentialVO, S extends IReferentialEntity> Optional<T> toTypedVO(S source, Class<T> targetClazz);

    ReferentialVO save(ReferentialVO source);

    void delete(String entityName, int id);

    Long count(String entityName);

    Long countByLevelId(String entityName, Integer... levelIds);

    <T> TypedQuery<T> createFindQuery(Class<T> entityClass,
                                      ReferentialFilterVO filter,
                                      String sortAttribute,
                                      SortDirection sortDirection,
                                      QueryVisitor<T, T> queryVisitor);
}
