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
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.IWithDescriptionAndCommentEntity;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationArea;
import net.sumaris.core.model.referential.location.LocationLine;
import net.sumaris.core.model.referential.location.LocationPoint;
import net.sumaris.rdf.config.RdfConfiguration;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


@Component("baseSchemaEquivalences")
@ConditionalOnBean({RdfConfiguration.class})
@ConditionalOnProperty(
        prefix = "rdf.equivalences",
        name = {"enabled"},
        matchIfMissing = true)
public class BaseSchemaEquivalences extends AbstractSchemaEquivalences {

    private static final Logger log = LoggerFactory.getLogger(BaseSchemaEquivalences.class);

    private boolean debug;


    @PostConstruct
    protected void init() {
        super.init();
        debug = log.isDebugEnabled();


    }

    @Override
    public void visitModel(Model model, String ns, String schemaUri) {
        log.info("Adding {{}} equivalences to {{}}...", basePrefix, schemaUri);

        if (model instanceof OntModel) {
            // Add Geometry -> SpatialObject
           // OntModel ontModel = (OntModel)model;
            //ontModel.createClass()
        }
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

        Resource schema = model.getResource(schemaService.getNamespace());

        // Entity
        if (IEntity.class.isAssignableFrom(clazz)) {

            ontClass.addProperty(RDF.type, SKOS.Concept)
                    .addProperty(SKOS.inScheme, schema);

            // ID
//            model.getResource(classUri + "#" + IEntity.Fields.ID)
//                    .addProperty(equivalentProperty, DC.identifier)
//                    .addProperty(equivalentProperty, DCTerms.identifier);

            // Update entity
            if (IUpdateDateEntityBean.class.isAssignableFrom(clazz)) {

                // Update date
                model.getResource(classUri + "#" + IUpdateDateEntityBean.Fields.UPDATE_DATE)
                        .addProperty(equivalentProperty, DCTerms.modified);

                // Referential entity
                if (IReferentialWithStatusEntity.class.isAssignableFrom(clazz)) {

                    // Creation date
                    model.getResource(classUri + "#" + IItemReferentialEntity.Fields.CREATION_DATE)
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

            if (IWithDescriptionAndCommentEntity.class.isAssignableFrom(clazz)) {

                // Description
                model.getResource(classUri + "#" + IWithDescriptionAndCommentEntity.Fields.DESCRIPTION)
                        .addProperty(equivalentProperty, DC.description)
                        .addProperty(equivalentProperty, DCTerms.description);

                // Comment
                model.getResource(classUri + "#" + IWithDescriptionAndCommentEntity.Fields.COMMENTS)
                        .addProperty(equivalentProperty, RDFS.comment);
            }
        }
    }

    protected void addSpecificEquivalences(Model model, Resource ontClass, Class clazz) {
        String classUri = ontClass.getURI();

        // Person
        if (clazz == Person.class) {

            // = FOAF.Person, FOAF.OnlineAccount
            ontClass.addProperty(equivalentClass, FOAF.Person)
                    .addProperty(equivalentClass, FOAF.OnlineAccount);

            // First name
            model.getResource(classUri + "#" + Person.Fields.FIRST_NAME)
                    .addProperty(equivalentProperty, FOAF.firstName);

            // Last name
            model.getResource(classUri + "#" + Person.Fields.LAST_NAME)
                    .addProperty(equivalentProperty, FOAF.lastName)
                    .addProperty(equivalentProperty, FOAF.familyName);

            // Email
            model.getResource(classUri + "#" + Person.Fields.EMAIL)
                    .addProperty(equivalentProperty, FOAF.mbox);

            // Email hash (MD5)
            //model.getResource(classUri + "#" + Person.Fields.EMAIL_M_D5)
            //        .addProperty(subPropertyOf, FOAF.mbox_sha1sum); // TODO: MD5 not found

            // pubkey = OnlineAccount.accountName
            model.getResource(classUri + "#" + Person.Fields.PUBKEY)
                    .addProperty(equivalentProperty, FOAF.accountName);

            // Avatar = image
            model.getResource(classUri + "#" + Person.Fields.AVATAR)
                    .addProperty(equivalentProperty, FOAF.img);

            // Avatar = image
            model.getResource(classUri + "#" + Person.Fields.DEPARTMENT)
                    .addProperty(equivalentProperty, FOAF.member);
        }

        // Department
        else if (clazz == Department.class) {
            // = Organization
            ontClass.addProperty(equivalentClass, FOAF.Organization);

            // Home page
            model.getResource(classUri + "#" + Department.Fields.SITE_URL)
                    .addProperty(equivalentProperty, FOAF.homepage);
        }

        // Location
        else if (clazz == Location.class) {
            // = SpatialThing
            ontClass.addProperty(equivalentClass, org.w3.GEO.WGS84Pos.SpatialThing);
        }

        // Location Area
        else if (clazz == LocationPoint.class) {
            // = Geometry
            model.getResource(classUri + "#" + LocationArea.Fields.POSITION)
                    .addProperty(equivalentClass, GEO.NAMESPACE + "Geometry");
        }
        else if (clazz == LocationLine.class) {
            // = Geometry + LineString
            model.getResource(classUri + "#" + LocationArea.Fields.POSITION)
                    .addProperty(equivalentClass, GEO.NAMESPACE + "Geometry")
                    .addProperty(equivalentClass, "http://www.opengis.net/ont/sf#LineString");
        }
        else if (clazz == LocationArea.class) {
            // = Geometry + Polygonal surface
            model.getResource(classUri + "#" + LocationArea.Fields.POSITION)
                    .addProperty(equivalentClass, GEO.NAMESPACE + "Geometry")
                    .addProperty(equivalentClass, "http://www.opengis.net/ont/sf#PolyhedralSurface");
        }

        // Program
        else if (clazz == Program.class) {
            // = Project
            ontClass.addProperty(equivalentClass, FOAF.Project);
        }
    }

    @Override
    public void visitIndividual(Model model, Resource instance, Class clazz) {

        String individualUri = instance.getURI();

        // ID
        instance
                .addProperty(DC.identifier, individualUri)
                .addProperty(DCTerms.identifier, individualUri);
    }
}