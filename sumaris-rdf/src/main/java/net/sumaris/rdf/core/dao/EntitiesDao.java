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
import net.sumaris.rdf.core.model.ModelVocabulary;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Stream;

public interface EntitiesDao {

    <T> T getById(ModelVocabulary domain, String className, Class<T> aClass, Serializable id);

    <T> Stream<T> streamAll(ModelVocabulary domain, String className, Class<T> aClass);

    <T> Stream<T> streamAll(ModelVocabulary domain, String className, Class<T> aClass, Page page);

    Set<String> getClassNamesByDomain(ModelVocabulary domain);

    ModelVocabulary getDomainByClassName(String className);

    Set<String> getClassNamesByRootClass(@Nullable ModelVocabulary domain, String className);
}
