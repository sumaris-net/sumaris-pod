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
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.service.schema.RdfSchemaService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("foafSchemaEquivalences")
@ConditionalOnBean({RdfConfiguration.class})
@ConditionalOnProperty(
        prefix = "rdf.equivalences",
        name = {"foaf.enabled"},
        matchIfMissing = true)
@Slf4j
public class FoafSchemaEquivalences extends AbstractSchemaVisitor {

    public FoafSchemaEquivalences(RdfSchemaService rdfSchemaService) {
        super(rdfSchemaService);
    }

    @Override
    public void visitModel(Model model, String ns, String schemaUri) {
        if (log.isDebugEnabled()) log.debug("Adding {{}} equivalences to {{}}...", "foaf", schemaUri);
    }

    @Override
    public void visitClass(Model model, Resource ontClass, Class clazz) {
        String classUri = ontClass.getURI();

        // Program
        if (clazz == Program.class) {
            if (log.isDebugEnabled()) log.debug("Adding {{}} equivalence on Class {{}}...", "foaf", clazz.getSimpleName());

            // = Project
            ontClass.addProperty(equivalentClass, FOAF.Project);
        }

        // Person
        else if (clazz == Person.class) {
            if (log.isDebugEnabled()) log.debug("Adding {{}} equivalence on Class {{}}...", "foaf", clazz.getSimpleName());

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
            if (log.isDebugEnabled()) log.debug("Adding {{}} equivalence on Class {{}}...", "foaf", clazz.getSimpleName());

            // = Organization
            ontClass.addProperty(equivalentClass, FOAF.Organization);

            // Home page
            model.getResource(classUri + "#" + Department.Fields.SITE_URL)
                    .addProperty(equivalentProperty, FOAF.homepage);
        }



    }
}
