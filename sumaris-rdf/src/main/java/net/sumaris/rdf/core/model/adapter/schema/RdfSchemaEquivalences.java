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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.ITreeNodeEntityBean;
import net.sumaris.core.dao.technical.model.IUpdateDateEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.IWithDescriptionAndCommentEntity;
import net.sumaris.rdf.core.config.RdfConfiguration;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


@Component("rdfSchemaEquivalences")
@ConditionalOnBean({RdfConfiguration.class})
@ConditionalOnProperty(
        prefix = "rdf.equivalences",
        name = {"enabled"},
        matchIfMissing = true)
@Slf4j
public class RdfSchemaEquivalences extends AbstractSchemaVisitor {

    private boolean debug;


    @PostConstruct
    protected void init() {
        super.init();
        debug = log.isDebugEnabled();


    }

    @Override
    public void visitModel(Model model, String ns, String schemaUri) {
        if (debug) log.debug("Adding {{}} equivalences to {{}}...", basePrefix, schemaUri);

        if (model instanceof OntModel) {
            // Add Geometry -> SpatialObject
           // OntModel ontModel = (OntModel)model;
            //ontModel.createClass()
        }
    }

    @Override
    public void visitClass(Model model, Resource ontClass, Class clazz) {
        model.setNsPrefix(basePrefix, baseUri);

        if (debug) log.debug("Adding {{}} equivalence on Class {{}}...", basePrefix, clazz.getSimpleName());
        String classUri = ontClass.getURI();

        Resource schema = model.getResource(schemaService.getNamespace());

        // Entity
        if (IEntity.class.isAssignableFrom(clazz)) {

            ontClass.addProperty(RDF.type, SKOS.Concept)
                    .addProperty(SKOS.inScheme, schema);

            // ID
            model.getResource(classUri + "#" + IEntity.Fields.ID)
                    .addProperty(equivalentProperty, DC.identifier)
                    .addProperty(equivalentProperty, DCTerms.identifier);

            // Update entity
            if (IUpdateDateEntity.class.isAssignableFrom(clazz)) {

                // Update date
                model.getResource(classUri + "#" + IUpdateDateEntity.Fields.UPDATE_DATE)
                        .addProperty(equivalentProperty, org.purl.DC.modified)
                        .addProperty(equivalentProperty, DCTerms.modified);

                // Referential entity
                if (IReferentialWithStatusEntity.class.isAssignableFrom(clazz)) {

                    // Creation date
                    model.getResource(classUri + "#" + IItemReferentialEntity.Fields.CREATION_DATE)
                            .addProperty(equivalentProperty, org.purl.DC.created)
                            .addProperty(equivalentProperty, DCTerms.created);

                    // Item referential
                    if (IItemReferentialEntity.class.isAssignableFrom(clazz)) {

                        // Label (WARN = code)
                        // TODO
                        //model.getResource(classUri + "#" + IItemReferentialEntity.Fields.LABEL)
                        // .addProperty(subPropertyOf, ??);

                        // Name
                        model.getResource(classUri + "#" + IItemReferentialEntity.Fields.NAME)
                                .addProperty(equivalentProperty, RDFS.label);
                    }
                }
            }

            // Description, comments
            if (IWithDescriptionAndCommentEntity.class.isAssignableFrom(clazz)) {

                // Description
                model.getResource(classUri + "#" + IWithDescriptionAndCommentEntity.Fields.DESCRIPTION)
                        .addProperty(equivalentProperty, DC.description)
                        .addProperty(equivalentProperty, DCTerms.description);

                // Comment
                model.getResource(classUri + "#" + IWithDescriptionAndCommentEntity.Fields.COMMENTS)
                        .addProperty(equivalentProperty, RDFS.comment);
            }

            // Tree node entity
            if (ITreeNodeEntityBean.class.isAssignableFrom(clazz)) {

                // Parent
                model.getResource(classUri + "#" + ITreeNodeEntityBean.Fields.PARENT)
                        .addProperty(equivalentProperty, SKOS.broader);
            }
        }
    }

    @Override
    public void visitIndividual(Model model, Resource instance, Class clazz) {


        // ID
        instance.addProperty(DC.identifier, instance.getURI());
    }
}