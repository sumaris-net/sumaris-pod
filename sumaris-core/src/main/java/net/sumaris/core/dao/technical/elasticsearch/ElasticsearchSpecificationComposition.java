/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package net.sumaris.core.dao.technical.elasticsearch;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.lang.Nullable;

import java.io.Serializable;

public class ElasticsearchSpecificationComposition {

    @FunctionalInterface
    interface Combiner<T extends QueryBuilder> extends Serializable {

        T combine(@Nullable T lhs, @Nullable T rhs);

    }

    static <T extends QueryBuilder> T must(@Nullable T query, @Nullable T other) {
        if (query == null) return other;
        if (other == null) return query;
        if (query instanceof BoolQueryBuilder) {
            return (T)((BoolQueryBuilder) query).must(other);
        }
        if (other instanceof BoolQueryBuilder) {
            return (T)((BoolQueryBuilder) other).must(query);
        }

        return (T)QueryBuilders.boolQuery()
            .must(query)
            .must(other);
    }

    static <T extends QueryBuilder> T filter(@Nullable T query,  @Nullable T other) {
        if (query == null) return other;
        if (other == null) return query;
        if (query instanceof BoolQueryBuilder) {
            return (T)((BoolQueryBuilder) query).filter(other);
        }
        if (other instanceof BoolQueryBuilder) {
            return (T)((BoolQueryBuilder) other).filter(query);
        }

        return (T)QueryBuilders.boolQuery()
            .filter(query)
            .filter(other);
    }

    static <T extends QueryBuilder> T should(T query,  T other) {
        if (query == null) return other;
        if (other == null) return query;
        if (query instanceof BoolQueryBuilder) {
            return (T)((BoolQueryBuilder) query).should(other);
        }
        if (other instanceof BoolQueryBuilder) {
            return (T)((BoolQueryBuilder) other).should(query);
        }

        return (T)QueryBuilders.boolQuery()
            .should(query)
            .should(other);
    }

    static <T extends QueryBuilder> ElasticsearchSpecification<T> composed(@Nullable ElasticsearchSpecification<T> lhs,
                                                                           @Nullable ElasticsearchSpecification<T> rhs,
                                                                           Combiner<T> combiner) {

        return () -> {

            T thisPredicate = toPredicate(lhs);
            T otherPredicate = toPredicate(rhs);

            if (thisPredicate == null) {
                return otherPredicate;
            }

            return otherPredicate == null ? thisPredicate : combiner.combine(thisPredicate, otherPredicate);
        };
    }

    @Nullable
    private static <T extends QueryBuilder> T toPredicate(
        @Nullable ElasticsearchSpecification<T> specification) {
        return specification == null ? null : specification.toPredicate();
    }
}