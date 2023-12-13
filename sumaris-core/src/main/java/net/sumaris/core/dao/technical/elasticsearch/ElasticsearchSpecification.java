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

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@FunctionalInterface
public interface ElasticsearchSpecification<T extends QueryBuilder> extends Serializable {

    static <T extends QueryBuilder> ElasticsearchSpecification<T> bool() {
        return () -> (T)QueryBuilders.boolQuery();
    }

    static <T extends QueryBuilder> ElasticsearchSpecification<T> constantScore(@Nullable ElasticsearchSpecification<T> spec) {
        return spec == null ? () -> null : () -> {
            T other = spec.toPredicate();
            if (other == null)  return null;
            return (T)QueryBuilders.constantScoreQuery(other);
        };
    }

    default ElasticsearchSpecification<T> must(@Nullable ElasticsearchSpecification<T> spec) {
        return spec == null ? this : ElasticsearchSpecificationComposition.composed(this, spec, ElasticsearchSpecificationComposition::must);
    }

    default ElasticsearchSpecification<T> filter(@Nullable ElasticsearchSpecification<T> spec) {
        return spec == null ? this : ElasticsearchSpecificationComposition.composed(this, spec, ElasticsearchSpecificationComposition::filter);
    }

    default ElasticsearchSpecification<T> should(@Nullable ElasticsearchSpecification<T> spec) {
        return spec == null ? this : ElasticsearchSpecificationComposition.composed(this, spec, ElasticsearchSpecificationComposition::should);
    }

    @Nullable T toPredicate();
}
