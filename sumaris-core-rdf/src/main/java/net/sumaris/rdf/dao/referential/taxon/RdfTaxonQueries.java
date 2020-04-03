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

package net.sumaris.rdf.dao.referential.taxon;

import net.sumaris.core.dao.technical.Page;
import org.apache.jena.atlas.lib.StrUtils;

/**
 * Helper class
 */
public class RdfTaxonQueries {
    public static final String CONSTRUCT_QUERY = StrUtils.strjoinNL(
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>",
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>",
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>",
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>",
            "PREFIX dwc: <http://rs.tdwg.org/dwc/terms/>",
            "PREFIX dwctn: <http://rs.tdwg.org/ontology/voc/TaxonName#>",
            "PREFIX apt: <http://id.eaufrance.fr/ddd/APT/>",
            "PREFIX apt2: <http://id.eaufrance.fr/ddd/APT/2.1/>",
            "PREFIX taxref: <http://taxref.mnhn.fr/lod/>",
            "PREFIX taxrefprop: <http://taxref.mnhn.fr/lod/property/>",
            "CONSTRUCT {",
            "    ?sub dwc:scientificName ?label ;",
            "      rdf:type dwctn:TaxonName ;",
            "      skos:broader ?parent ;" +
            "      dc:author ?author ;" +
            "      dc:created ?created ;" +
            "      dc:modified ?modified ;" +
            "      taxrefprop:hasRank ?rank ;" +
            "      owl:sameAs ?match ;",
            "      rdfs:seeAlso ?seeAlso .",
            "}",
            "WHERE { ",
            "  ?sub dwc:scientificName ?label ;",
            "       rdf:type ?type .",
            "  FILTER ( ?type = dwctn:TaxonName || URI(?type) = apt:AppelTaxon )",
            "  OPTIONAL {",
            "      ?sub taxrefprop:hasAuthority|apt2:AuteurAppelTaxon ?author ;\n" +
            "         taxrefprop:hasRank|apt2:NiveauTaxonomique ?rank .\n",
            "    }",
            "  OPTIONAL {",
            "      ?sub skos:exactMatch|owl:sameAs ?match .",
            "      FILTER ( isURI(?match) )",
            "    }",
            "  OPTIONAL {",
            "      ?sub rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .",
            "      FILTER ( isURI(?seeAlso) )",
            "    }",
            "  OPTIONAL {",
            "      ?sub dc:created|apt2:DateCreationAppelTaxon ?created ;",
            "          dc:modified|apt2:DateMajAppelTaxon ?modified .",
            "    }",
            "  OPTIONAL {",
            "      ?sub skos:broader|apt2:AppelTaxonParent ?parent .",
            "    }",
            "}"
    );

    public static final String LIMIT_CLAUSE = "LIMIT %s OFFSET %s";


    public static String getConstructQuery(Page page) {
        if (page == null || page.getSize() < 0 || page.getOffset() < 0) return CONSTRUCT_QUERY;
        return CONSTRUCT_QUERY + "\n" + String.format(LIMIT_CLAUSE, page.getSize(), page.getOffset());
    }


}
