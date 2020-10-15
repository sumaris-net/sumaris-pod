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

package org.purl;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DC_11;

/**
 * Darwin Core (v1.1).
 * We redeclare this class, because we need 'dc:modified' and 'dc:created', used by some partners
 *
 * @deprecated used DCTerms instead, when possible
 */
@Deprecated
public class DC {

    public static final String NS = DC_11.NS;
    public static final String PREFIX = "dc";
    public static String getURI() {
        return NS;
    }

    protected static Resource resource(String local) {
        return ResourceFactory.createResource(NS + local);
    }

    protected static Property property(String local) {
        return ResourceFactory.createProperty(NS + local);
    }

    /**
     * @deprecated used DCTerms.modified instead, when possible
     */
    @Deprecated
    public static final Resource modified = resource(NS + "modified");

    /**
     * @deprecated used DCTerms.created instead, when possible
     */
    @Deprecated
    public static final Resource created = resource(NS + "created");
}
