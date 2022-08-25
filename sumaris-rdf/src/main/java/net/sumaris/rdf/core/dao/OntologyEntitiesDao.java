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

package net.sumaris.rdf.core.dao;

import net.sumaris.core.dao.technical.Page;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface OntologyEntitiesDao {

    <T> T getById(String vocabulary, String ontClassName, Class<T> aClass, Serializable id);

    <T> T getByLabel(String vocabulary, String ontClassName, Class<T> aClass, String label);

    <T> T getByProperty(String vocabulary, String ontClassName, Class<T> aClass, String propertyName, Object propertyValue);

    <T> Stream<T> streamAll(String vocabulary, String ontClassName);

    <T> Stream<T> streamAll(String vocabulary, String ontClassName, Page page);

    Set<String> getAllClassNamesByVocabulary(String vocabulary);

    Set<Class<?>> getAllTypesByVocabulary(String vocabulary);

    Class<?> getTypeByVocabularyAndClassName(String vocabulary, String ontClassName);

    String getVocabularyByClassName(String ontClassName);

    Optional<String> findTypeUri(Type type);

    Set<String> getAllVocabularies();
}
