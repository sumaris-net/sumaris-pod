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
    const prefixDefs = [
        {
            name: 'this',
            prefix: 'this',
            namespace: undefined
        }].concat(helper.constants.prefixes);

    const exampleQueries = [
        // default query
        {
            name: "Simple",
            prefixes: ['rdf', 'dwc', 'dwctax', 'this'],
            query: "SELECT DISTINCT *\n" +
                "WHERE {\n" +
                "  ?sub rdf:type ?type ; \n" +
                "       dwc:scientificName ?label .  \n" +
                "  # Filter by name\n" +
                "  FILTER( \n" +
                "    ( ?type=sar:TaxonName || ?type=dwctax:TaxonName ) \n" +
                "     && regex( ?label, \"^Lophius\" ) \n" +
                "  )\n" +
                "} LIMIT 10"
        },
        {
            name: "Taxon by name (Sandre)",
            endpoint: endpointsById.EAU_FRANCE,
            prefixes: ['dc', 'rdf', 'rdfs', 'owl', 'skos', 'foaf', 'apt', 'apt2', 'aptdata', 'taxref'],
            query: "SELECT DISTINCT ?tax ?label ?exactMatch \n" +
                "WHERE {\n" +
                "  ?tax rdf:type ?type ;\n" +
                "    rdfs:label ?label ;\n" +
                "    owl:sameAs ?exactMatch .\n" +
                "  # Filter by name\n" +
                "  FILTER( " +
                "    regex( ?label, \"^lophius.*\", \"i\") \n" +
                "    && URI(?type) = <http://rs.tdwg.org/dwc/terms/Taxon> \n" +
                "  )\n" +
                "}\n" +
                "LIMIT 100"
        },
        {
            name: "Taxon by code (Sandre)",
            endpoint: endpointsById.EAU_FRANCE,
            prefixes: ['dc', 'rdf', 'rdfs', 'owl', 'skos', 'foaf', 'apt', 'apt2', 'aptdata'],
            query: "SELECT *\n" +
                "WHERE {\n" +
                "  <http://id.eaufrance.fr/apt/18996> ?pred ?obj\n" +
                "}"
        },
        {
            name: "Taxon by code (TaxRef)",
            endpoint: endpointsById.MNHN,
            prefixes: ['dc', 'rdf', 'rdfs', 'owl', 'foaf', 'skos', 'dwc', 'dwciri', 'dwctax', 'taxref'],
            query: "SELECT *\n" +
                "WHERE {\n" +
                "  <http://taxref.mnhn.fr/lod/name/194272> ?pred ?obj .\n" +
                "} LIMIT 100"
        },
        {
            name: "Taxon by name (TaxRef)",
            endpoint: endpointsById.MNHN,
            prefixes: ['dc', 'rdf', 'rdfs', 'owl', 'foaf', 'skos', 'dwc', 'dwciri', 'dwctax', 'taxref'],
            query: "SELECT DISTINCT *\n" +
                "WHERE {\n" +
                "  ?tax dwc:scientificName ?label ;\n" +
                "    rdf:type dwctax:TaxonName ;\n" +
                "    ?pred ?obj .\n" +
                "  # Filter by name\n" +
                "  FILTER(\n" +
                "    regex( ?label, \"^lophius.*\", \"i\")\n" +
                "  )\n" +
                "} LIMIT 100"
        },
        {
            name: "Organization by name (Sandre)",
            endpoint: endpointsById.EAU_FRANCE,
            prefixes: ['dc', 'rdf', 'rdfs', 'owl', 'foaf', 'inc1', 'inc', 'incdata'],
            query: "SELECT DISTINCT\n" +
                "  *\n" +
                "WHERE {\n" +
                "  ?sub rdf:type inc:Interlocuteur ;\n" +
                "    rdfs:label ?label ;\n" +
                "    ?pred ?obj .\n" +
                "  # Filter by name\n" +
                "  FILTER(\n" +
                "    regex( ?label, \"^.*environnement.*\", \"i\")\n" +
                "  )\n" +
                "}\n" +
                "ORDER BY ?sub\n" +
                "LIMIT 100"
        }
    ];

    function init() {

        if (window.location && window.location.origin) {

            // Compute default endpoint
            defaultEndpoint = window.location.origin + '/sparql';

            endpointsById.THIS = defaultEndpoint;
        }

        // Update the default prefix
        helper.loadDefaultPrefix((prefix) => {
            prefixDefs[0] = {
                ...prefixDefs[0],
                ...prefix};
        });

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
        const links = prefixDefs.reduce((res, def, index) => {
            if (!def.name || !def.prefix || !def.namespace) return res; // Skip if no name (e.g. default)
            return res.concat("<a href=\"#\" title=\""+def.name+"\" onclick=\"app.addPrefix("+index+")\">"+def.prefix+"</a>");
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

        const prefixes = (example.prefixes || [])
            .map(p => p === 'this' ? prefixDefs[0].prefix : p) // Replace 'this' by default prefic
            .reduce((res, prefix) => {

            const def = prefixDefs.find(def => def.prefix === prefix);
            res[prefix] = def && def.namespace || undefined;
            return res;
        }, {});
        tab.yasqe.addPrefixes(prefixes);

        return tab;
    }

    function addPrefix(index) {
        const def = prefixDefs[index];
        if (!def || !def.prefix || !def.namespace) return; // Skip
        console.debug("Adding prefix: ", def.prefix);

        let tab;
        // Add a new Tab. Returns the new Tab object.
        if (yasgui.tabElements._selectedTab) {
            tab = yasgui.getTab(yasgui.tabElements._selectedTab);
        }
        else {
            tab = showExampleQuery();
        }

        const prefix = {};
        prefix[def.prefix] = def.namespace;
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
