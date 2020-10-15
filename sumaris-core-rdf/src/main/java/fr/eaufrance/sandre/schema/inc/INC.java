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

package fr.eaufrance.sandre.schema.inc;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.w3.GEO;

/**
 * Appellation taxon du SANDRE
 *
 * see http://www.sandre.eaufrance.fr/atlas/srv/fre/catalog.search#/metadata/0bea2eb9-a4ee-4a81-b494-bd4fb5338ef8
 */
public class INC {
    public static final String NS = "http://id.eaufrance.fr/ddd/INC/";

    public static final String PREFIX = "inc";
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

    public static final Resource Interlocuteur = resource("Interlocuteur");

    public static final Resource AdresseInterlocuteur = resource("1.0/#AdresseInterlocuteur");
    public static final Resource Compl2Adresse = resource("1.0/Compl2Adresse");
    public static final Resource Compl3Adresse = resource("1.0/Compl3Adresse");
    public static final Resource NumLbVoieAdresse = resource("1.0/NumLbVoieAdresse");
    public static final Resource LgAcheAdresse = resource("1.0/LgAcheAdresse");
    public static final Resource PaysInterlocuteur = resource("1.0/PaysInterlocuteur");
    public static final Resource CdPays = resource("1.0/CdPays");
    public static final Resource DateCreInterlocuteur = resource("1.0/DateCreInterlocuteur");
    public static final Resource DateMAJInterlocuteur = resource("1.0/DateMAJInterlocuteur");

}
