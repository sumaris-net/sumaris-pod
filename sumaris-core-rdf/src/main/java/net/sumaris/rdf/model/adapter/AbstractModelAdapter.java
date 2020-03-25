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

package net.sumaris.rdf.model.adapter;

import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.IWithDescriptionAndCommentEntity;
import net.sumaris.rdf.model.IModelVisitor;
import net.sumaris.rdf.service.schema.RdfSchemaExportService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.Objects;


public abstract class AbstractModelAdapter implements IModelVisitor {

    @Autowired
    protected RdfSchemaExportService exportSchemaService;

    @Value("${rdf.adapter.default.owl.enabled:true}")
    protected boolean enableOwlAdapter;

    protected Property subClassOf = RDFS.subClassOf;
    protected Property subPropertyOf =  RDFS.subPropertyOf;
    protected String basePrefix = org.eclipse.rdf4j.model.vocabulary.RDFS.PREFIX;
    protected String baseUri = org.eclipse.rdf4j.model.vocabulary.RDFS.NAMESPACE;

    @PostConstruct
    protected void init() {
        // Register to schema service
        exportSchemaService.register(this);

        if (this.enableOwlAdapter) {
            subClassOf = OWL2.equivalentClass;
            subPropertyOf = OWL2.equivalentProperty;
            basePrefix = OWL.PREFIX;
            baseUri = OWL2.NS;
        }
    }

    @Override
    public boolean accept(Model model, String ns, String schemaUri) {
        return Objects.equals(exportSchemaService.getOntologySchemaUri(), schemaUri);
    }

}