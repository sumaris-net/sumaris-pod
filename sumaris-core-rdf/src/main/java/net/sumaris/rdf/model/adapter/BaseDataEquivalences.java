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
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.IWithDescriptionAndCommentEntity;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationArea;
import net.sumaris.core.model.referential.location.LocationLine;
import net.sumaris.core.model.referential.location.LocationPoint;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.model.IModelVisitor;
import net.sumaris.rdf.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.service.data.RdfDataExportOptions;
import net.sumaris.rdf.service.data.RdfDataExportService;
import net.sumaris.rdf.service.schema.RdfSchemaOptions;
import net.sumaris.rdf.service.schema.RdfSchemaService;
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
import java.util.Objects;


@Component("baseDataEquivalences")
@ConditionalOnBean({RdfConfiguration.class})
@ConditionalOnProperty(
        prefix = "rdf.equivalences",
        name = {"enabled"},
        matchIfMissing = true)
public class BaseDataEquivalences implements IModelVisitor<Model, RdfDataExportOptions> {

    private static final Logger log = LoggerFactory.getLogger(BaseDataEquivalences.class);

    private boolean debug;

    @javax.annotation.Resource
    protected RdfDataExportService dataExportService;

    @javax.annotation.Resource
    protected RdfSchemaService schemaService;

    @PostConstruct
    protected void init() {
        // Register to schema service
        dataExportService.register(this);
        debug = log.isDebugEnabled();
    }

    @Override
    public boolean accept(Model model, String prefix, String namespace, RdfDataExportOptions options) {
        return options.getReasoningLevel() != ReasoningLevel.NONE && Objects.equals(schemaService.getNamespace(), namespace);
    }

    @Override
    public void visitIndividual(Model model, Resource instance, Class clazz) {

        String individualUri = instance.getURI();

        // ID
        instance.addProperty(DC_11.identifier, individualUri);
    }

    /* -- protected methods -- */
}