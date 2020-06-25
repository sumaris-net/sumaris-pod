/*-
 * #%L
 * SUMARiS:: RDF features
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
function RdfHelper() {

    const constants = {
            prefixes: [
                {
                    name: 'Dublin Core',
                    prefix: 'dc',
                    namespace: 'http://purl.org/dc/elements/1.1/'
                },
                {
                    name: 'Friend of a Firiend',
                    prefix: 'foaf',
                    namespace: 'http://xmlns.com/foaf/0.1/'
                },
                {
                    name: 'OWL',
                    prefix: 'owl',
                    namespace: 'http://www.w3.org/2002/07/owl#'
                },
                {
                    name: 'RDF',
                    prefix: 'rdf',
                    namespace: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#'
                },
                // {
                //     name: 'RDF Data',
                //     prefix: 'rdfdata',
                //     namespace: 'http://rdf.data-vocabulary.org/rdf.xml#'
                // },
                // {
                //     name: 'RDF Data format',
                //     prefix: 'rdfdf',
                //     namespace: 'http://www.openlinksw.com/virtrdf-data-formats#'
                // },
                // {
                //     name: 'RDF FG',
                //     prefix: 'rdfg',
                //     namespace: 'http://www.w3.org/2004/03/trix/rdfg-1/'
                // },
                // {
                //     name: 'RDF FP',
                //     prefix: 'rdfp',
                //     namespace: 'https://w3id.org/rdfp/'
                // },
                {
                    name: 'RDF Schema',
                    prefix: 'rdfs',
                    namespace: 'http://www.w3.org/2000/01/rdf-schema#'
                },
                {
                    name: 'Darwin core terms (string literal objects)',
                    prefix: 'dwc',
                    namespace: 'http://rs.tdwg.org/dwc/terms/'
                },
                {
                    name: 'Darwin core terms (IRI reference objects)',
                    prefix: 'dwciri',
                    namespace: 'http://rs.tdwg.org/dwc/iri/'
                },
                {
                    name: 'Darwin core TaxonName',
                    prefix: 'dwctax',
                    namespace: 'http://rs.tdwg.org/ontology/voc/TaxonName#'
                },
                {
                    name: 'SKOS thesaurus',
                    prefix: 'skos',
                    namespace: 'http://www.w3.org/2004/02/skos/core#'
                },
                // Spatial
                {
                    name: 'Spatial',
                    prefix: 'spatial',
                    namespace: 'http://www.w3.org/2004/02/skos/core#'
                },
                {
                    name: 'GeoSparql',
                    prefix: 'geo',
                    namespace: 'http://www.opengis.net/ont/geosparql#'
                },
                {
                    name: 'GeoNames',
                    prefix: 'gn',
                    namespace: 'http://www.geonames.org/ontology#'
                },

                // Custom (Taxon specific)
                {
                    name: 'TaxRef linked data (MNHN)',
                    prefix: 'taxref',
                    namespace: 'http://taxref.mnhn.fr/lod/'
                },
                {
                    name: 'TaxRef properties (MNHN)',
                    prefix: 'taxrefprop',
                    namespace: 'http://taxref.mnhn.fr/lod/property/'
                },
                {
                    name: 'Eau France (Sandre)',
                    prefix: 'eaufrance',
                    namespace: 'http://id.eaufrance.fr/'
                },
                {
                    name: 'Appellation Taxon (Sandre)',
                    prefix: 'apt',
                    namespace: 'http://id.eaufrance.fr/ddd/APT/'
                },
                {
                    name: 'Appellation Taxon (Sandre) v2.1',
                    prefix: 'apt2',
                    namespace: 'http://id.eaufrance.fr/ddd/APT/2.1/'
                },
                {
                    name: 'Appellation Taxon (Sandre) data ',
                    prefix: 'aptdata',
                    namespace: 'http://id.eaufrance.fr/apt/'
                },
            ]
    };

    function loadNodeInfo(successCallback) {
        $.ajax({
            url: "/api/node/info/",
            cache: true,
            dataType: 'json',
            success: (res) => {
                if (successCallback) successCallback(res);
            }
        });
    }

    function loadDefaultPrefix(callback) {
        if (!callback) throw new Error("Missing 'callback' argument.");
        loadNodeInfo(res => {
            const namespace = window.location.origin + '/ontology/schema/';
            const prefix  = res && res.nodeLabel && res.nodeLabel.toLowerCase()|| examplePrefixes[0].prefix;
            callback({
                namespace,
                prefix
            });
        });
    }

    return {
        constants,
        loadDefaultPrefix
    };
}
