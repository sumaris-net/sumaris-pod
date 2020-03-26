function RdfHelper() {

    const constants = {
            prefixes: [
                {
                    name: 'Dublin Core',
                    ns: 'dc',
                    prefix: 'http://purl.org/dc/elements/1.1/'
                },
                {
                    name: 'Friend of a Firiend',
                    ns: 'foaf',
                    prefix: 'http://xmlns.com/foaf/0.1/'
                },
                {
                    name: 'OWL',
                    ns: 'owl',
                    prefix: 'http://www.w3.org/2002/07/owl#'
                },
                {
                    name: 'RDF',
                    ns: 'rdf',
                    prefix: 'http://www.w3.org/1999/02/22-rdf-syntax-ns#'
                },
                {
                    name: 'RDFA',
                    ns: 'rdfa',
                    prefix: 'http://www.w3.org/ns/rdfa#'
                },
                {
                    name: 'RDF Data',
                    ns: 'rdfdata',
                    prefix: 'http://rdf.data-vocabulary.org/rdf.xml#'
                },
                {
                    name: 'RDF Data format',
                    ns: 'rdfdf',
                    prefix: 'http://www.openlinksw.com/virtrdf-data-formats#'
                },
                {
                    name: 'RDF FG',
                    ns: 'rdfg',
                    prefix: 'http://www.w3.org/2004/03/trix/rdfg-1/'
                },
                {
                    name: 'RDF FP',
                    ns: 'rdfp',
                    prefix: 'https://w3id.org/rdfp/'
                },
                {
                    name: 'RDF Schema',
                    ns: 'rdfs',
                    prefix: 'http://www.w3.org/2000/01/rdf-schema#'
                },
                {
                    name: 'Appellation Taxon (Sandre)',
                    ns: 'apt',
                    prefix: 'http://owl.sandre.eaufrance.fr/apt/2.1/sandre_fmt_owl_apt.owl'
                },
                {
                    name: 'Darwin core terms (string literal objects)',
                    ns: 'dwc',
                    prefix: 'http://rs.tdwg.org/dwc/terms/'
                },
                {
                    name: 'Darwin core terms (IRI reference objects)',
                    ns: 'dwciri',
                    prefix: 'http://rs.tdwg.org/dwc/iri/'
                },
                {
                    name: 'LOD (TaxRef)',
                    ns: 'lod',
                    prefix: 'http://taxref.mnhn.fr/lod/'
                },
                {
                    name: 'SKOS thesaurus',
                    ns: 'skos',
                    prefix: 'http://www.w3.org/2004/02/skos/core#'
                },
                // Spatial
                {
                    name: 'Spatial',
                    ns: 'spatial',
                    prefix: 'http://www.w3.org/2004/02/skos/core#'
                },
                {
                    name: 'GeoSparql',
                    ns: 'geo',
                    prefix: 'http://www.opengis.net/ont/geosparql#'
                },
                {
                    name: 'GeoNames',
                    ns: 'gn',
                    prefix: 'http://www.geonames.org/ontology#'
                },
            ]
    };

    return {
        constants
    };
}
