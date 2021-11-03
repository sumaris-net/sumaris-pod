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
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.rdf.core.config.RdfConfiguration;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.tdwg.rs.DWC;

@Component("dwcSchemaEquivalences")
@ConditionalOnBean({RdfConfiguration.class})
@ConditionalOnProperty(
        prefix = "rdf.equivalences",
        name = {"dwc.enabled"},
        matchIfMissing = true)
@Slf4j
public class DwcSchemaEquivalences extends AbstractSchemaVisitor {

    @Override
    public void visitModel(Model model, String ns, String schemaUri) {
        if (log.isDebugEnabled()) log.debug("Adding {{}} equivalences to {{}}...", DWC.Terms.PREFIX, schemaUri);
    }

    @Override
    public void visitClass(Model model, Resource ontClass, Class clazz) {
        String classUri = ontClass.getURI();

        // Reference Taxon
        if (clazz == ReferenceTaxon.class) {
            if (log.isDebugEnabled()) log.debug("Adding {{}} equivalence on Class {{}}...", DWC.Terms.PREFIX, clazz.getSimpleName());

            ontClass.addProperty(equivalentClass, DWC.Terms.Taxon);

            // Id
            model.getResource(classUri + "#" + ReferenceTaxon.Fields.ID)
                    .addProperty(equivalentProperty, DWC.Terms.taxonID);
        }

        // Taxon Name
        else if (clazz == TaxonName.class) {
            if (log.isDebugEnabled()) log.debug("Adding {{}} equivalence on Class {{}}...", DWC.Terms.PREFIX, clazz.getSimpleName());

            ontClass.addProperty(equivalentClass, DWC.Voc.TaxonName);

            // Id
            model.getResource(classUri + "#" + ReferenceTaxon.Fields.ID)
                    .addProperty(equivalentProperty, DWC.Terms.scientificNameID);

            // Complete name
            model.getResource(classUri + "#" + TaxonName.Fields.NAME)
                .addProperty(equivalentProperty, DWC.Terms.scientificName);
        }
    }
}
