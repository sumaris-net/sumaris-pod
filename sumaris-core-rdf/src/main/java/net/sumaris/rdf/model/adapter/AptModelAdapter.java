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

import fr.eaufrance.sandre.schema.apt.APT;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.model.IModelVisitor;
import net.sumaris.rdf.service.schema.RdfSchemaExportService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

@Component("aptModelAdapter")
@ConditionalOnBean({RdfConfiguration.class})
@ConditionalOnProperty(
        prefix = "rdf",
        name = {"adapter.sandre.enabled"},
        matchIfMissing = true)
public class AptModelAdapter implements IModelVisitor {

    private static final Logger log = LoggerFactory.getLogger(AptModelAdapter.class);

    @Autowired
    private RdfSchemaExportService service;

    @PostConstruct
    public void init() {
        service.register(this);
    }

    @Override
    public boolean accept(Model model, String ns, String schemaUri) {
        return Objects.equals(service.getOntologySchemaUri(), schemaUri);
    }

    @Override
    public void visitSchema(Model model, String ns, String schemaUri) {
        log.info("Adding {{}} equivalences to {{}}...", APT.PREFIX, schemaUri);
    }

    @Override
    public void visitClass(Model model, Resource onClass, Class clazz) {
        if (TaxonName.class == clazz) {
            log.info("Adding {{}} equivalence on Class {{}}...", APT.PREFIX, clazz.getSimpleName());

        }
    }
}
