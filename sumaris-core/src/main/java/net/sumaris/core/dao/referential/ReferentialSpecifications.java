package net.sumaris.core.dao.referential;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.technical.jpa.SpecificationWithParameters;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.metier.Metier;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;

public class ReferentialSpecifications {

    public static Specification<Metier> inLevelIds(String levelProperty, Integer[] gearIds) {
        if (ArrayUtils.isEmpty(gearIds)) return null;
        return (root, query, cb) -> cb.in(
                root.join(levelProperty, JoinType.INNER).get(Gear.PROPERTY_ID))
                .value(ImmutableList.copyOf(gearIds));
    }

    public static <T extends IReferentialEntity> Specification<T> inStatusIds(Integer[] statusIds) {
        if (ArrayUtils.isEmpty(statusIds)) return null;
        return (root, query, cb) -> cb.in(
                root.get(IReferentialEntity.PROPERTY_STATUS).get(Status.PROPERTY_ID))
                .value(ImmutableList.copyOf(statusIds));
    }

    public static <T extends IItemReferentialEntity> Specification<T> searchText(String searchAttribute, String paramName) {

        return new SpecificationWithParameters<T>() {

            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query,
                                         CriteriaBuilder cb) {
                ParameterExpression<String> searchTextParam = add(cb.parameter(String.class, paramName));

                if (StringUtils.isNotBlank(searchAttribute)) {
                    return cb.or(
                            cb.isNull(searchTextParam),
                            cb.like(cb.upper(root.get(searchAttribute)), cb.upper(searchTextParam)));
                }
                // Search on label+name
                return cb.or(
                        cb.isNull(searchTextParam),
                        cb.like(cb.upper(root.get(IItemReferentialEntity.PROPERTY_LABEL)), cb.upper(searchTextParam)),
                        cb.like(cb.upper(root.get(IItemReferentialEntity.PROPERTY_NAME)), cb.upper(cb.concat("%", searchTextParam)))
                );
            }
        };
    }

    public static <T extends IItemReferentialEntity> Specification<T> joinSearchText(String joinProperty, String searchAttribute, String paramName) {

        return new SpecificationWithParameters<T>() {

            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query,
                                         CriteriaBuilder cb) {
                ParameterExpression<String> searchTextParam = add(cb.parameter(String.class, paramName));

                if (StringUtils.isNotBlank(searchAttribute)) {
                    return cb.or(
                            cb.isNull(searchTextParam),
                            cb.like(cb.upper(root.join(joinProperty).get(searchAttribute)), cb.upper(searchTextParam)));
                }
                // Search on label+name
                return cb.or(
                        cb.isNull(searchTextParam),
                        cb.like(cb.upper(root.join(joinProperty)
                                .get(IItemReferentialEntity.PROPERTY_LABEL)), cb.upper(searchTextParam)),
                        cb.like(cb.upper(root.join(joinProperty)
                                .get(IItemReferentialEntity.PROPERTY_NAME)), cb.upper(cb.concat("%", searchTextParam)))
                );
            }
        };
    }
}
