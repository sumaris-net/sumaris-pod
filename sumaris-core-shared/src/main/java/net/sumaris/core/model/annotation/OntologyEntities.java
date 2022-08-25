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

package net.sumaris.core.model.annotation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IModel;
import net.sumaris.core.util.StringUtils;
import org.reflections.Reflections;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class for enumerations
 */
public final class OntologyEntities {


    protected OntologyEntities(){
        // Helper class
    }

    public static Set<Class<?>> getOntologyEntityClasses(SumarisConfiguration config) {

        // Add annotations entities
        Reflections reflections = (config != null && config.isProduction() ? Reflections.collect() : new Reflections(IModel.MODEL_PACKAGE_NAME));
        return reflections.getTypesAnnotatedWith(OntologyEntity.class);
    }

    public static Collection<Definition> getOntologyEntityDefs(SumarisConfiguration config,
                                                               String defaultVocabulary,
                                                               String defaultVersion) {
        return OntologyEntities.getOntologyEntityClasses(config).stream()
            .map(entityClass -> {
                // Get annotation detail
                final OntologyEntity annotation = entityClass.getAnnotation(OntologyEntity.class);
                String vocabulary = StringUtils.isBlank(annotation.vocab()) ? defaultVocabulary : annotation.vocab();
                String name = StringUtils.isBlank(annotation.name()) ? entityClass.getSimpleName() : annotation.name();
                if (name.substring(1).contains("<")) {
                    name = name.substring(0, name.indexOf("<"));
                }

                String version = StringUtils.isBlank(annotation.version()) ? defaultVersion : annotation.version();
                String namedQuery = StringUtils.isNotBlank(annotation.namedQuery()) ? annotation.namedQuery() : null;
                String query = StringUtils.isNotBlank(annotation.query()) ? annotation.query() : null;
                if (namedQuery != null && query != null) {
                   throw new SumarisTechnicalException("Invalid annotation @OntologyEntity() on " + entityClass.getSimpleName() + ". Cannot have both 'namedQuery' and 'query'!");
                }
                return Definition.builder()
                    .vocabulary(vocabulary)
                    .name(name)
                    .version(version)
                    .type(entityClass)
                    .query(query)
                    .query(namedQuery)
                    .build();
            }).collect(Collectors.toList());
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class Definition {
        private String vocabulary;
        private String name;
        private String version;
        private Class<?> type;
        private String query;
        private String namedQuery;
    }
}
