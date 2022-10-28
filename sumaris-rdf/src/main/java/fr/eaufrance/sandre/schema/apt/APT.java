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

package fr.eaufrance.sandre.schema.apt;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Appellation taxon du SANDRE
 *
 * see http://www.sandre.eaufrance.fr/atlas/srv/fre/catalog.search#/metadata/0bea2eb9-a4ee-4a81-b494-bd4fb5338ef8
 */
public class APT {
    public static final String NS = "http://id.eaufrance.fr/ddd/apt/2.1/";
    public static final String PREFIX = "apt";
    public static final String MODEL_URL = "http://owl.sandre.eaufrance.fr/apt/2.1/sandre_fmt_owl_apt.owl";

    public static String getURI() {
        return NS;
    }

    protected final static Resource resource(String local) {
        return ResourceFactory.createResource(NS + local);
    }

    protected final static Property property(String local) {
        return ResourceFactory.createProperty(NS + local);
    }

    public static final Resource NAMESPACE = resource(NS);

    public static final Resource AppelTaxon = resource("AppelTaxon");

}
