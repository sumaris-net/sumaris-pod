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

package net.sumaris.rdf.core.model.adapter.data;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.model.IModelVisitor;
import net.sumaris.rdf.core.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.core.service.data.RdfIndividualFetchOptions;
import net.sumaris.rdf.core.service.data.RdfIndividualService;
import net.sumaris.rdf.core.service.schema.RdfSchemaService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;


@Component("rdfIndividualEquivalences")
@ConditionalOnBean({RdfConfiguration.class})
@ConditionalOnProperty(
        prefix = "rdf.equivalences",
        name = {"enabled"},
        matchIfMissing = true)
@Slf4j
public class RdfIndividualEquivalences implements IModelVisitor<Model, RdfIndividualFetchOptions> {

    private boolean debug;

    @javax.annotation.Resource
    protected RdfIndividualService individualService;

    @javax.annotation.Resource
    protected RdfSchemaService schemaService;

    @PostConstruct
    protected void init() {
        // Register to individual service
        individualService.register(this);

        debug = log.isDebugEnabled();
    }

    @Override
    public boolean accept(Model model, String prefix, String namespace, RdfIndividualFetchOptions options) {
        return options.getReasoningLevel() != ReasoningLevel.NONE && Objects.equals(schemaService.getNamespace(), namespace);
    }

    @Override
    public void visitIndividual(Model model, Resource instance, Class clazz) {

        String individualUri = instance.getURI();

        // ID
        instance.addProperty(DC.identifier, individualUri)
                .addProperty(DCTerms.identifier, individualUri);
    }

    /* -- protected methods -- */
}