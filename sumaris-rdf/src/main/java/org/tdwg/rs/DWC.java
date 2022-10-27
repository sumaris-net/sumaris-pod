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

package org.tdwg.rs;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class DWC {

    public static class Voc {
        public static final String NS = "http://rs.tdwg.org/ontology/voc/";
        public static String getURI() {
            return NS;
        }

        protected final static Resource resource(String local) {
            return ResourceFactory.createResource(NS + local);
        }

        protected final static Property property(String local) {
            return ResourceFactory.createProperty(NS + local);
        }

        public static final Resource TaxonName = resource("TaxonName#TaxonName");

    }

    public static class Terms {
        public static final String URI = "http://rs.tdwg.org/dwc/terms/";
        public static final String PREFIX = "dwc";
        public static String getURI() {
            return URI;
        }

        protected final static Resource resource(String local) {
            return ResourceFactory.createResource(URI + local);
        }

        protected final static Property property(String local) {
            return ResourceFactory.createProperty(URI + local);
        }

        public static final Resource Taxon = resource("Taxon");
        public static final Property taxonID = property("taxonID");
        public static final Property scientificName = property("scientificName");
        public static final Property scientificNameID = property("scientificNameID");
    }
}
