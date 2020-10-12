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

package org.w3;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class W3NS {

    public static final String NS = "http://www.w3.org/ns/";
    public static final String PREFIX = "ns";
    public static String getURI() {
        return NS;
    }

    public static class Org {
        public static final String NS = W3NS.NS + "org#";
        public static final String PREFIX = "org";
        public static String getURI() {
            return NS;
        }

        protected static Resource resource(String local) {
            return ResourceFactory.createResource(NS + local);
        }

        protected static Property property(String local) {
            return ResourceFactory.createProperty(NS + local);
        }

        public static final Resource Organization = resource("Organization");

        public static final Resource Site = resource("Site");
        public static final Resource hasprimarySite = resource("hasprimarySite");
        public static final Resource siteAddress = resource("siteAddress");

    }
}
