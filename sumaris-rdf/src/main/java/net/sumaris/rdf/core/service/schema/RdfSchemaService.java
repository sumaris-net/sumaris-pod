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

package net.sumaris.rdf.core.service.schema;

import net.sumaris.rdf.core.cache.RdfCacheConfiguration;
import net.sumaris.rdf.core.model.IModelVisitor;
import net.sumaris.rdf.core.service.IRdfFetchOptions;
import net.sumaris.rdf.core.service.data.RdfIndividualFetchOptions;
import org.apache.jena.rdf.model.Model;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Transactional(readOnly = true)
public interface RdfSchemaService {

    String getPrefix();

    String getNamespace();

    String getNamespace(String vocabulary);

    String getNamespace(String vocabulary, String version);

    /**
     * Register a schema visitor
     * @param visitor
     */
    void register(IModelVisitor<Model, RdfSchemaFetchOptions> visitor);

    /**
     * Get schema, as an ontology (classes with properties)
     * @param options export options
     * @return a schema representation as an ontology
     */
    Model getOntology(RdfSchemaFetchOptions options);

    Model getOntology(String vocabulary);


    Set<String> getAllVocabularies();

    Model getAllOntologies();

    /**
     * Get classes to export when accessing individuals
     * @param options the export context
     * @return
     */
    Set<Class<?>> getAllTypes(RdfIndividualFetchOptions options);

    Set<String> getAllClassNames(IRdfFetchOptions options);
}
