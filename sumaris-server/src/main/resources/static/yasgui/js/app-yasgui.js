function AppYasgui(yasGuiDivId) {

    let defaultEndpoint,
        defaultPrefixUri,
        endpoints,
        yasgui;

    const endpointsById = {
        THIS: 'this',
        EAU_FRANCE: 'http://id.eaufrance.fr/sparql',
        MNHN: 'http://taxref.mnhn.fr/sparql'
    };

    const examplePrefixes = [
        {
            name: 'this',
            ns: 'this',
            prefix: undefined
        },
        {
            name: 'Dublin Core',
            ns: 'dc',
            prefix: 'http://purl.org/dc/elements/1.1/'
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
            name: 'RDFS',
            ns: 'rdfs',
            prefix: 'http://www.w3.org/2000/01/rdf-schema#'
        },
        {
            name: 'Appel Taxon',
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
            name: 'LOD',
            ns: 'lod',
            prefix: 'http://taxref.mnhn.fr/lod/'
        },
        {
            name: 'SKOS thesaurus',
            ns: 'skos',
            prefix: 'http://www.w3.org/2004/02/skos/core#'
        }
    ];

    const exampleQueries = [
        // default query
        {
            name: "default",
            prefixes: ['rdf', 'this'],
            query: "SELECT * WHERE {\n" +
                "  ?sub ?pred ?obj .\n" +
                "  ?sub rdf:type this:TaxonName .  \n" +
                "  filter( ?obj=\"Lophius budegassa\" )\n" +
                "} LIMIT 10"
        },
        {
          name: "Sandre: Taxon by ID",
          endpoint: endpointsById.EAU_FRANCE,
          query: "SELECT *\n" +
              "WHERE {\n" +
              "  <http://id.eaufrance.fr/apt/18996> ?pred ?obj\n" +
              "}"
        },
        {
          name: "Sandre: Taxon by name",
          endpoint: endpointsById.EAU_FRANCE,
          prefixes: ['dc', 'rdf', 'rdfs', 'owl'],
          query: "SELECT *\n" +
              "WHERE {\n" +
              "  ?tax rdf:type \"http://rs.tdwg.org/dwc/terms/Taxon\" .\n" +
              "  ?tax rdfs:label ?label .\n" +
              "  ?tax ?pred ?obj\n" +
              "  filter(?label=\"Lophius\")\n" +
              "}\n" +
              "LIMIT 100"
        },
        {
            name: "Sandre: Taxons by regexp",
            endpoint: endpointsById.EAU_FRANCE,
            prefixes: ['dc', 'rdf', 'rdfs', 'owl'],
            query: "SELECT *\n" +
                "WHERE {\n" +
                "  ?tax rdf:type \"http://rs.tdwg.org/dwc/terms/Taxon\" .\n" +
                "  ?tax rdfs:label ?label .\n" +
                "  ?tax owl:sameAs ?reftax .\n" +
                "  filter( regex( ?label, \"^lophius.*\", \"i\") )\n" +
                "}\n" +
                "LIMIT 100"
        },
        {
            name: "MNHN: Taxon by name",
            endpoint: endpointsById.MNHN,
            prefixes: ['dc', 'dwc', 'dwciri', 'rdf', 'rdfs', 'owl', 'skos'],
            query: "SELECT\n" +
                "  *\n" +
                "WHERE {\n" +
                "  ?tax dwc:scientificName ?label .\n" +
                "  ?tax rdf:type <http://rs.tdwg.org/ontology/voc/TaxonName#TaxonName> .\n" +
                "  ?tax ?pred ?obj\n" +
                "  filter(?label=\"Lophius\")\n" +
                "} LIMIT 1000"
        },
        {
            name: "MNHN: Taxon by regexp",
            endpoint: endpointsById.MNHN,
            prefixes: ['dc', 'dwc', 'dwciri', 'rdf', 'rdfs', 'owl', 'skos'],
            query: "SELECT DISTINCT\n" +
                "  *\n" +
                "WHERE {\n" +
                "  ?tax dwc:scientificName ?label .\n" +
                "  ?tax rdf:type <http://rs.tdwg.org/ontology/voc/TaxonName#TaxonName> .\n" +
                "  ?tax ?pred ?obj\n" +
                "  filter( regex( ?label, \"^lophius.*\", \"i\") )\n" +
                "} LIMIT 1000"
        }
    ];

    function init() {

        if (window.location && window.location.origin) {

            // Compute default endpoint
            defaultEndpoint = window.location.origin + '/sparql';

            endpointsById.THIS = defaultEndpoint

            // Compute default URI
            defaultPrefixUri = window.location.origin + '/ontology/schema/';
            examplePrefixes[0].prefix = defaultPrefixUri;
        }

        // Collect endpoints, from default, endpoint map, and examples queries
        endpoints = Object.keys(endpointsById).map(key => endpointsById[key])
            .concat(Object.keys(exampleQueries).map(key => exampleQueries[key].endpoint))
            .reduce((res, ep) => (!ep || res.findIndex(endpoint => endpoint === ep) !== -1) ? res : res.concat(ep),
                [defaultEndpoint]);

        connect();
    }

    function connect() {

        // Start YasGUI
        yasgui = new Yasgui(document.getElementById(yasGuiDivId), {
            requestConfig: {
                endpoint: defaultEndpoint || endpoints.length && endpoints[0] || undefined
                // headers: () => ({
                //     //'token': '' // TODO
                // })
            },
            copyEndpointOnNewTab: true,
            endpointCatalogueOptions: {
                getData: getEndpoints
            }
        });

    }

    function getEndpoints() {
        console.debug("get Endpoints");
        return endpoints;
    }

    function disconnect() {
        yasgui = null;
        document.getElementById(yasGuiDivId).innerHTML = "";
    }

    function displayExamples(elementId) {

        // Queries
        const links = exampleQueries.reduce(function(res, example, index) {
            if (!example.name) return res; // Skip if no name (e.g. default)
            return res.concat("<a href=\"#\" onclick=\"app.showExampleQuery("+index+")\">"+example.name+"</a>");
        }, []);

        const innerHTML = links.length && ("Examples: " + links.join(" | ") + "<br/>") || "";

        const element = document.getElementById(elementId);
        element.innerHTML = innerHTML;
    }

    function displayPrefixes(elementId) {

        // Prefixes
        const links = examplePrefixes.reduce((res, example, index) => {
            if (!example.name || !example.ns) return res; // Skip if no name (e.g. default)
            return res.concat("<a href=\"#\" title=\""+example.name+"\" onclick=\"app.addPrefix("+index+")\">"+example.ns+"</a>");
        }, []);

        const innerHTML = links.length &&  ("Prefixes: " + links.join(" | "));

        const element = document.getElementById(elementId);
        element.innerHTML = innerHTML;
    }

    function showExampleQuery(index) {
        const example = exampleQueries[index||0];
        if (!example) return; // Skip
        console.debug("Use example query: ", example);

        // Add a new Tab. Returns the new Tab object.
        const tab = yasgui.addTab(
            true, // set as active tab
            { ...Yasgui.Tab.getDefaults(),
                name: example.name,
                requestConfig: {
                    endpoint: example.endpoint || defaultEndpoint
                }
            }
        );

        tab.setQuery(example.query);

        const prefixes = {};
        (example.prefixes || []).forEach(ns => {
            const example = examplePrefixes.find(p => p.ns === ns);
            prefixes[ns] = example && example.prefix || undefined;
        });
        tab.yasqe.addPrefixes(prefixes);

        return tab;
    }

    function addPrefix(index) {
        const example = examplePrefixes[index];
        if (!example || !example.ns || !example.prefix) return; // Skip
        console.debug("Adding prefix: ", example);

        let tab;
        // Add a new Tab. Returns the new Tab object.
        if (yasgui.tabElements._selectedTab) {
            tab = yasgui.getTab(yasgui.tabElements._selectedTab);
        }
        else {
            tab = showExampleQuery();
        }

        const prefix = {};
        prefix[example.ns] = example.prefix;
        tab.yasqe.addPrefixes(prefix);

    }



    window.addEventListener("load", init, false);

    const exports = {
        connect,
        disconnect,
        displayExamples,
        displayPrefixes,
        showExampleQuery,
        addPrefix
    };

    return exports;
}