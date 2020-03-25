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

    const helper = new RdfHelper();
    const examplePrefixes = [
        {
            name: 'this',
            ns: 'this',
            prefix: undefined
        }].concat(helper.constants.prefixes);

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

            endpointsById.THIS = defaultEndpoint;

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
                //     //'token': '' // TODO add authenticated token
                // })
            },
            copyEndpointOnNewTab: true,
            endpointCatalogueOptions: {
                getData: getEndpoints
            }
        });

    }

    function getEndpoints() {
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