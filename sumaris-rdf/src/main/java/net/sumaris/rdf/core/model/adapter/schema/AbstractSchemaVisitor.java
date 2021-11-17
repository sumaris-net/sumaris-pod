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

package net.sumaris.rdf.core.model.adapter.schema;

import net.sumaris.rdf.core.model.IModelVisitor;
import net.sumaris.rdf.core.service.schema.RdfSchemaFetchOptions;
import net.sumaris.rdf.core.service.schema.RdfSchemaService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Objects;


public abstract class AbstractSchemaVisitor implements IModelVisitor<Model, RdfSchemaFetchOptions> {

    @Resource
    protected RdfSchemaService schemaService;

    @Value("${rdf.equivalences.owl.enabled:false}")
    protected boolean useOwlEquivalences;

    protected Property equivalentClass = RDFS.subClassOf;
    protected Property equivalentProperty =  RDFS.subPropertyOf;
    protected String basePrefix = org.eclipse.rdf4j.model.vocabulary.RDFS.PREFIX;
    protected String baseUri = org.eclipse.rdf4j.model.vocabulary.RDFS.NAMESPACE;

    @PostConstruct
    protected void init() {
        // Register to schema service
        schemaService.register(this);

        if (this.useOwlEquivalences) {
            equivalentClass = OWL2.equivalentClass;
            equivalentProperty = OWL2.equivalentProperty;
            basePrefix = OWL.PREFIX;
            baseUri = OWL2.NS;
        }
    }

    @Override
    public boolean accept(Model model, String prefix, String namespace, RdfSchemaFetchOptions options) {
        return options.isWithEquivalences() && Objects.equals(schemaService.getNamespace(), namespace);
    }

}