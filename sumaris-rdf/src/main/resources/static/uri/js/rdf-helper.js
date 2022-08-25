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
                // Common schemas
                {
                    name: 'Dublin Core',
                    prefix: 'dc',
                    namespace: 'http://purl.org/dc/elements/1.1/'
                },
                {
                    name: 'Dublin Core Terms',
                    prefix: 'dcterms',
                    namespace: 'http://purl.org/dc/terms/'
                },
                {
                    name: 'Schema.org',
                    prefix: 's',
                    namespace: 'http://schema.org/'
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
                {
                    name: 'RDF Schema',
                    prefix: 'rdfs',
                    namespace: 'http://www.w3.org/2000/01/rdf-schema#'
                },
                {
                    name: 'SKOS thesaurus',
                    prefix: 'skos',
                    namespace: 'http://www.w3.org/2004/02/skos/core#'
                },

                // Social
                {
                    name: 'Friend of a Friend',
                    prefix: 'foaf',
                    namespace: 'http://xmlns.com/foaf/0.1/'
                },
                {
                    name: 'Good Relations',
                    prefix: 'gr',
                    namespace: 'http://purl.org/goodrelations/v1#'
                },
                {
                    name: 'XML Schema Datatypes',
                    prefix: 'xsd',
                    namespace: 'http://www.w3.org/2001/XMLSchema#'
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


                // Taxon
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

                // Taxon (custom)
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
                    name: 'Appellation Taxon (Sandre)',
                    prefix: 'apt2',
                    namespace: 'http://id.eaufrance.fr/ddd/APT/2.1/'
                },
                {
                    name: 'Appellation Taxon (Sandre) data ',
                    prefix: 'aptdata',
                    namespace: 'http://id.eaufrance.fr/apt/'
                },

                // Organization
                {
                    name: 'Organization (W3C)',
                    prefix: 'org',
                    namespace: 'http://www.w3.org/ns/org#'
                },

                // Organization (custom)
                {
                    name: 'Interlocuteurs (Sandre)',
                    prefix: 'inc',
                    namespace: 'http://id.eaufrance.fr/ddd/INC/1.0/'
                },
                {
                    name: 'Interlocuteurs (Sandre) data ',
                    prefix: 'incdata',
                    namespace: 'http://id.eaufrance.fr/inc/'
                },

                // Commun (Sandre)
                {
                    name: 'Commun (Sandre)',
                    prefix: 'com',
                    namespace: 'http://id.eaufrance.fr/ddd/COM/4/'
                },
                {
                    name: 'Statut Sandre',
                    prefix: 'status',
                    namespace: 'http://id.eaufrance.fr/nsa/727#'
                },
                {
                    name: 'Validity Statut Sandre',
                    prefix: 'validityStatus',
                    namespace: 'http://id.eaufrance.fr/nsa/390#'
                },
                {
                    name: 'SIREN Vocab',
                    prefix: 'sirene',
                    namespace: 'https://sireneld.io/vocab/sirene#'
                }
            ]
    };

    function loadNodeHealth(options) {
        const cache = (typeof options === 'object') ? options.cache : true;
        const success = (typeof options === 'object') ? options.success : options;
        const error = (typeof options === 'object') ? options.error : undefined;
        console.info('[rdf-helper] Loading node health \'/api/node/health\' ...');
        $.ajax({
            url: "/api/node/health",
            dataType: 'json',
            cache,
            success: (res) => {
                console.info('[rdf-helper] Node health loaded:', res);
                if (typeof success === 'function') success(res);
            },
            error: (err) => {
                if (typeof error === 'function') error(err);
                else console.error('[rdf-helper] Error while getting node health', err);
            }
        });
    }

    function loadDefaultPrefix(options, defaultPrefix) {
        if (!options) throw new Error("Missing 'options' argument.");

        const success = (typeof options === 'object') ? options.success : options;
        if (!success) throw new Error("Missing 'options.success' argument.");

        const error = (typeof options === 'object') ? options.error : (err) => {
            console.error("[rdf-helper] Cannot load node default prefix: ", err && err.responseText || err);
        }
        defaultPrefix = defaultPrefix || 'this';
        const defaultNamespace = window.location.origin + '/schema/';
        const defaultVersion = '0.1';
        loadNodeHealth({
            success: (res) => {
                const details = res && res.components && res.components.rdf && res.components.rdf.details;
                const namespace = details && details.schemaUri || defaultNamespace;
                const prefix = details && details.modelPrefix || defaultPrefix;
                const version = details && details.modelVersion || defaultVersion;
                success({
                    namespace,
                    prefix,
                    version
                });
            },
            error
        });
    }

    /**
     * Execute a spqral GET query. By default, use CSV and return a string[]
     * @param query
     * @param options
     */
    function executeSparql(query, options) {
        if (!query) throw new Error('Missing \'sparql\' argument!');
        options = options || {};
        options.url = options.url || '/sparql';
        options.cache = options.cache !== false;
        options.contentType = options.contentType || 'application/sparql-results+csv';
        options.dataType = options.dataType || 'text';

        $.ajax({
            data: {
                query,
                type: 'GET',
                ...options.data
            },
            ...options,
            success: (result) => {
                // Convert to array
                if (options.contentType === 'application/sparql-results+csv') {
                    result = result.replaceAll('"', '').split('\n')
                      // Exclude header row, and the last (empty) line
                      .filter((line, index) => index > 0 && line && line.length)
                      // Remove double quote
                      .map(line => line.replaceAll('"', ''));
                }
                if (typeof options.success === 'function') options.success(result);
            },
            error: (err) => {
                if (typeof options.error === 'function') options.error(err);
                else console.error(err);
            }
        });
    }

    function executeSparqlCount(query, options) {
        options = options || {}
        return executeSparql(query, {
            success: (result) => {
                // PArse the result, as an unique number result
                const count = result && parseInt(result[0]) || -1;
                console.info('[rdf-helper] Sparql count result: ' + count);
                if (typeof options.success === 'function') options.success(count);
            }
        });
    }

    /***
     *
     * @param query a SPARQL select query
     * @param options
     */
    function executeCountFromSelect(query, options) {
        if (!query) throw new Error('Missing \'sparql\' argument!');
        options = options || {};
        options.error = options.error || ((err) => {
            console.error("[rdf-helper] Error while executing count query: ", err);
        });

        console.info('[rdf-helper] Executing sparql count, from a select query...');

        // Create the count query, from a select query
        const countQuery = query
          // Transform SELECT into COUNT
          .replace(/SELECT[^{]+WHERE/gm, 'SELECT (COUNT(DISTINCT ?sourceUri) as ?count) WHERE')
          // Remove ORDER BY and LIMIT
          .replace(/ORDER BY[^{]+LIMIT[\s0-9]+/gm, '');

        // Execute the query
        return executeSparqlCount(countQuery, options);
    }

    return {
        constants,
        loadDefaultPrefix,
        executeSparql,
        executeSparqlCount,
        executeCountFromSelect
    };
}

// Add missing String.replaceAll() function (Need for compatibility with some web browsers - Fix issue #33)
if (typeof String.prototype.replaceAll !== 'function') {
    String.prototype.replaceAll = function ( search, replacement ){
        var target = this;
        return target.split(search).join(replacement);
    };
}
