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
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.model.IModelVisitor;
import net.sumaris.rdf.service.schema.RdfSchemaExportService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;


@Component("defaultModelAdapter")
@ConditionalOnBean({RdfConfiguration.class})
@ConditionalOnProperty(
        prefix = "rdf",
        name = {"adapter.default.enabled"},
        matchIfMissing = true)
public class DefaultModelAdapter extends AbstractModelAdapter {

    private static final Logger log = LoggerFactory.getLogger(DefaultModelAdapter.class);

    private boolean debug;


    @PostConstruct
    protected void init() {
        super.init();
        debug = log.isDebugEnabled();
    }

    @Override
    public void visitSchema(Model model, String ns, String schemaUri) {
        log.info("Adding {{}} equivalences to {{}}...", basePrefix, schemaUri);

    }

    @Override
    public void visitClass(Model model, Resource ontClass, Class clazz) {
        if (debug) log.debug("Adding {{}} equivalence on Class {{}}...", basePrefix, clazz.getSimpleName());

        // Generic equivalence, based on interfaces
        addGenericEquivalences(model, ontClass, clazz);

        // Specific equivalences
        addSpecificEquivalences(model, ontClass, clazz);

        model.setNsPrefix(basePrefix, baseUri);
    }

    /* -- protected methods -- */

    public void addGenericEquivalences(Model model, Resource ontClass, Class clazz) {
        String classUri = ontClass.getURI();

        // Entity
        if (IEntity.class.isAssignableFrom(clazz)) {
            model.getResource(classUri + "#" + IEntity.Fields.ID)
                    .addProperty(subPropertyOf, DC.identifier)
                    .addProperty(subPropertyOf, DCTerms.identifier);

            // Update entity
            if (IUpdateDateEntityBean.class.isAssignableFrom(clazz)) {

                // Update date
                model.getResource(classUri + "#" + IUpdateDateEntityBean.Fields.UPDATE_DATE)
                        .addProperty(subPropertyOf, DCTerms.modified);

                // Referential entity
                if (IReferentialEntity.class.isAssignableFrom(clazz)) {

                    // Creation date
                    model.getResource(classUri + "#" + IItemReferentialEntity.Fields.CREATION_DATE)
                            .addProperty(subPropertyOf, DCTerms.dateSubmitted);

                    // Item referential
                    if (IItemReferentialEntity.class.isAssignableFrom(clazz)) {

                        // Label (WARN = code)
                        // TODO
                        //model.getResource(classUri + "#" + IItemReferentialEntity.Fields.LABEL)
                        // .addProperty(subPropertyOf, ??);

                        // Name
                        model.getResource(classUri + "#" + IItemReferentialEntity.Fields.NAME)
                                .addProperty(subPropertyOf, RDFS.label);
                    }
                }
            }

            if (IWithDescriptionAndCommentEntity.class.isAssignableFrom(clazz)) {

                // Description
                model.getResource(classUri + "#" + IWithDescriptionAndCommentEntity.Fields.DESCRIPTION)
                        .addProperty(subPropertyOf, DC.description)
                        .addProperty(subPropertyOf, DCTerms.description);

                // Comment
                model.getResource(classUri + "#" + IWithDescriptionAndCommentEntity.Fields.COMMENTS)
                        .addProperty(subPropertyOf, RDFS.comment);
            }
        }
    }

    protected void addSpecificEquivalences(Model model, Resource ontClass, Class clazz) {
        String classUri = ontClass.getURI();

        // Person
        if (clazz == Person.class) {

            // = FOAF.Person, FOAF.OnlineAccount
            ontClass.addProperty(subClassOf, FOAF.Person)
                    .addProperty(subClassOf, FOAF.OnlineAccount);

            // First name
            model.getResource(classUri + "#" + Person.Fields.FIRST_NAME)
                    .addProperty(subPropertyOf, FOAF.firstName);

            // Last name
            model.getResource(classUri + "#" + Person.Fields.LAST_NAME)
                    .addProperty(subPropertyOf, FOAF.lastName)
                    .addProperty(subPropertyOf, FOAF.familyName);

            // Email
            model.getResource(classUri + "#" + Person.Fields.EMAIL)
                    .addProperty(subPropertyOf, FOAF.mbox);

            // Email hash (MD5)
            //model.getResource(classUri + "#" + Person.Fields.EMAIL_M_D5)
            //        .addProperty(subPropertyOf, FOAF.mbox_sha1sum); // TODO: MD5 not found

            // pubkey = OnlineAccount.accountName
            model.getResource(classUri + "#" + Person.Fields.PUBKEY)
                    .addProperty(subPropertyOf, FOAF.accountName);

            // Avatar = image
            model.getResource(classUri + "#" + Person.Fields.AVATAR)
                    .addProperty(subPropertyOf, FOAF.img);
        }


    }
}