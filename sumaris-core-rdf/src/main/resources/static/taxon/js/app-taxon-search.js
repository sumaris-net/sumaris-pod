

function AppTaxonSearch(config) {
    const defaultConfig = {
        yasqe: 'yasqe',
        yasr: 'yasr',
        tabs: 'tabs',
        optionsDiv: 'options',

        exactMatch: undefined,
        limit: 50,
        prefix: 'this'
    };

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


    const defaultQuery = "SELECT DISTINCT * WHERE {\n" +
        "  {{where}} \n" +
        "} LIMIT {{limit}}";

    const filtersMap = {
        // Schema filter
        rdfType: 'rdf:type {{rdfType}}',
        rdfsLabel: '?sourceUriUri rdfs:label ?scientificName .',
        dwcScientificName: '?sourceUriUri dwc:scientificName ?scientificName .',

        // Regex filter, on scientific name
        exactMatch: '?scientificName="{{q}}"',
        prefixMatch: 'regex( ?scientificName, "^{{q}}", "i" )',
        anyMatch: 'regex( ?scientificName, "{{q}}", "i" )',

        // Regex filter, on uri (code)
        codeExactMatch: 'strEnds( str(?sourceUri), "/{{q}}" )',
        codePrefixMatch: 'regex( str(?sourceUri), "/{{q}}$", "i" )'
    };
    const queries = [
        {
            id: 'local-name',
            name: 'Search by name',
            canHandleTerm: (term) => term && term.trim().match(/^[A-Za-z ]+$/),
            yasrPlugin : 'taxon',
            q: 'Lophius budegassa',
            prefixes: ['dc', 'rdf', 'owl', 'skos', 'foaf', 'dwc', 'dwctax', 'rdfs',
                'taxref', 'taxrefprop', 'apt', 'apt2', 'aptdata', 'this'],
            query: 'SELECT DISTINCT \n' +
                '  ?sourceUri ?scientificName ?parent ?author ?rank \n' +
                '  ?created ?modified \n' +
                '  ?exactMatch ?seeAlso ?created ?modified \n' +
                'WHERE {\n' +
                '  ?sourceUri dwc:scientificName ?scientificName ;\n' +
                '       rdf:type ?type .\n' +
                '  FILTER (\n' +
                '     {{filter}}\n' +
                '     && (?type = dwctax:TaxonName || ?type = {{defaultPrefix}}:TaxonName) \n' +
                '  ) .\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri skos:exactMatch|owl:sameAs ?exactMatch .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri skos:broader ?parent .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri dc:created ?created ;\n' +
                '      dc:modified ?modified .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri dc:author ?author .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri taxrefprop:hasRank ?rank .\n' +
                '  }' +
                '} LIMIT {{limit}}',
            filters: ['prefixMatch'],
            binding: {}
        },

        {
            id: 'local-code',
            name: 'Search by code',
            canHandleTerm: (term) => term && term.trim().match(/^[0-9]+$/),
            yasrPlugin : 'taxon',
            q: '847866',
            prefixes: ['dc', 'rdf', 'rdfs', 'owl', 'skos', 'foaf', 'dwc', 'dwctax',
                'taxref', 'taxrefprop', 'apt', 'apt2', 'aptdata', 'this'],
            query:  'SELECT DISTINCT \n' +
                '  ?sourceUri ?scientificName ?parent ?author ?rank \n' +
                '  ?created ?modified \n' +
                '  ?exactMatch ?seeAlso ?created ?modified \n' +
                'WHERE {\n' +
                '  ?sourceUri dwc:scientificName ?scientificName ;\n' +
                '       rdf:type ?type .\n' +
                '  FILTER (\n' +
                '     {{filter}}\n' +
                '     && (?type = dwctax:TaxonName || ?type = {{defaultPrefix}}:TaxonName) \n' +
                '  ) .\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri skos:exactMatch|owl:sameAs ?exactMatch .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri skos:broader ?parent .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri dc:created ?created ;\n' +
                '      dc:modified ?modified .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri dc:author ?author .\n' +
                '  }' +
                '  OPTIONAL {\n' +
                '    ?sourceUri taxrefprop:hasRank ?rank .\n' +
                '  }' +
                '} LIMIT {{limit}}',
            filters: ['codePrefixMatch'],
            binding: {
            }
        },

        {
            id: 'remote-name',
            name: 'Federated search by name',
            canHandleTerm: (term) => term && term.trim().match(/^[A-Za-z ]+$/),
            yasrPlugin : 'taxon',
            debug: false,
            q: 'Lophius budegassa',
            prefixes: ['dc', 'rdf',  'rdfs', 'owl', 'skos', 'foaf', 'dwc', 'dwctax',
                'taxref', 'taxrefprop', 'apt', 'apt2', 'aptdata', 'eaufrance'],
            query: 'SELECT DISTINCT \n' +
                '  ?sourceUri ?scientificName ?author ?rank ?parent \n' +
                '  ?created ?modified \n' +
                '  ?exactMatch ?seeAlso \n' +
                'WHERE {\n' +

                // -- MNHN endpoint part
                ' { SERVICE <{{mnhnEndpoint}}> {\n' +
                '    ?sourceUri dwc:scientificName ?scientificName ;\n' +
                '      rdf:type dwctax:TaxonName ;\n' +
                '      taxrefprop:hasAuthority ?author ;\n' +
                '      taxrefprop:hasRank ?rank \n' +
                '    FILTER(\n' +
                '       ( ?scientificName = "{{q}}" )\n' + // TODO: regexp not supported
                '    )\n' +
                '    OPTIONAL {\n' +
                '      ?sourceUri skos:broader ?parent .\n' +
                '    }\n' +
                '    OPTIONAL {\n' +
                '      ?sourceUri skos:exactMatch|owl:sameAs ?exactMatch .\n' +
                '    }\n' +
                '    OPTIONAL {\n' +
                '      ?sourceUri rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .\n' +
                '    }\n' +
                '    OPTIONAL {\n' +
                '      ?sourceUri dc:create ?created ;\n' +
                '        dc:modified ?modified .\n' +
                '    }\n' +
                '  } \n' +
                ' }\n' +
                ' UNION\n' +

                // -- Sandre endpoint part
                ' { SERVICE <{{eauFranceEndpoint}}> {\n' +
                '    ?sourceUri dwc:scientificName ?scientificName ;\n' +
                '      rdf:type ?type ;\n' +
                '      apt2:AuteurAppelTaxon ?author ;\n' +
                '      apt2:NiveauTaxonomique ?rank .\n' +
                '    FILTER(\n' +
                '      ( ?scientificName = "{{q}}" )\n' +
                '       && ( ?type = dwctax:TaxonName || URI(?type) = apt:AppelTaxon ) \n' +
                '    )\n' +
                '    OPTIONAL {\n' +
                '      ?sourceUri apt2:AppelTaxonParent|skos:broader ?parent .\n' +
                '      #FILTER ( isURI(?parent) )\n' +
                '    }\n' +
                '    OPTIONAL {\n' +
                '      ?sourceUri skos:exactMatch|owl:sameAs ?exactMatch .\n' +
                '      #FILTER ( isURI(?exactMatch) )\n' +
                '    }\n' +
                '    OPTIONAL {\n' +
                '      ?sourceUri rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .\n' +
                '      #FILTER ( isURI(?seeAlso) )\n' +
                '    }\n' +
                '    OPTIONAL {\n' +
                '      ?sourceUri dc:created|apt2:DateCreationAppelTaxon ?created ;\n' +
                '        dc:modified|apt2:DateMajAppelTaxon ?modified .\n' +
                '    }\n' +
                '  }\n' +
                ' }\n' +
                '} LIMIT {{limit}}',
            filters: [],
            binding: {
                eauFranceEndpoint: endpointsById.EAU_FRANCE,
                mnhnEndpoint: endpointsById.MNHN
            }
        },
        {
            id: 'rdfs',
            name: 'rdfs',
            debug: true,
            q: 'Lophius budegassa',
            prefixes: ['rdf', 'rdfs', 'this'],
            query: defaultQuery,
            filters: ['rdfType', 'rdfsLabel', 'prefixMatch'],
            binding: {
                rdfType: 'this:TaxonName'
            }
        },
        {
            id: 'dwc',
            name: 'dwc',
            debug: true,
            q: 'Lophius budegassa',
            prefixes: ['rdf', 'dwc',  'dwctax', 'this'],
            query: defaultQuery,
            filters: ['rdfType', 'dwcScientificName', 'prefixMatch'],
            binding: {
                rdfType: 'dwctax:TaxonName'
            }
        }
     ];

    // SparQL var
    let defaultEndpoint,
        defaultPrefixUri,
        endpoints;

    // Form elements
    let output,
        buttonSearch,
        inputSearch,
        selectedQueryIndex = -1;

    // YasGui
    let yasqe, yasr;

    /* -- Log and message -- */

    function onError(evt)
    {
        log('ERROR: ' + evt.data, 'text-error');
    }


    function log(message, classAttribute)
    {
        const pre = document.createElement("p");
        if (classAttribute) {
            const classes = classAttribute.split(" ");
            for (let i=0; i< classes.length; i++) {
                pre.classList.add(classes[i]);
            }
        }
        pre.style.wordWrap = "break-word";
        pre.innerHTML = message;
        output.appendChild(pre);

        output.classList.remove('d-none');

    }

    function clearLog()
    {
        output.innerHTML = "";
    }

    /* -- Init functions -- */

    function init() {
        console.debug("Init taxon search app...");

        config = {
            ...defaultConfig,
            ...config
        };

        if (window.location && window.location.origin) {

            // Compute default endpoint
            defaultEndpoint = window.location.origin + '/sparql';

            endpointsById.THIS = defaultEndpoint;

            // Update the default prefix
            defaultPrefixUri = window.location.origin + '/ontology/data/TaxonName/';
            helper.loadDefaultPrefix((prefixDef) => {
                prefixDefs[0] = {
                    ...prefixDefs[0],
                    namespace: defaultPrefixUri,
                    prefix: prefixDef.prefix + 'data'
                };
                prefixDefs.push(prefixDef);
            });
        }


        inputSearch = document.getElementById("q");
        inputSearch.addEventListener("keyup", function(event) {
            if (event.key === "Enter") {
                event.preventDefault();
                doSearch();
            }
        });

        buttonSearch = document.getElementById("buttonSearch");
        output = document.getElementById("output");

        // Collect endpoints, from default, endpoint map, and queries
        endpoints = Object.keys(endpointsById).map(key => endpointsById[key])
            .reduce((res, ep) => (!ep || res.findIndex(endpoint => endpoint === ep) !== -1) ? res : res.concat(ep),
                [defaultEndpoint]);

        // Add tabs
        app.drawTabs();
    }

    /**
     * Update the config (partially)
     * @param config
     */
    function setConfig(newConfig, options) {
        options = options || {};
        const oldConfigStr = JSON.stringify(config);
        config = {
            ...config,
            ...newConfig
        };
        const changed = JSON.stringify(config) !== oldConfigStr;

        // Something changed
        if (changed) {

            // re-run the search (if NOT silent moe)
            if (options.silent !== true && selectedQueryIndex !== -1) {
                const query = queries[selectedQueryIndex];
                if (inputSearch.value) {
                    doSearch(inputSearch.value, query);
                }

                // Or update the query
                else {
                    doUpdateQuery(query.q, query);
                }
            }
        }
    }

    /* -- Queries (as tabs) -- */

    function drawTabs(elementId) {

        elementId = elementId || config.tabs;
        config.tabs = elementId;

        // Queries
        const tabs = queries.map((example, index) => {
            if (!example.id) return res; // Skip if no name (e.g. default)
            const debugClassList = example.debug ? ['debug', 'd-none'].join(' ') : '';
            return '<li class="nav-item "'+ debugClassList + '">' +
                '<a href="#" class="nav-link '+ example.id + ' ' + debugClassList +'"' +
                ' onclick="app.selectQuery('+index+')">'+example.name+'</a></li>';
        }).join('\n');

        const innerHTML = tabs && "<ul class=\"nav nav-tabs\">" + tabs + "</ul>" || "";

        const element = document.getElementById(elementId);
        element.innerHTML = innerHTML;

    }

    function selectQuery(index, options) {
        options = options || {};
        if (selectedQueryIndex === index) return; // Skip if same

        const query = queries[index];
        if (query) {
            // Remember this choice
            selectedQueryIndex = index;
            inputSearch.placeholder = query.q;

            // re-run the search (if NOT silent moe)
            if (options.silent !== true) {
                if (inputSearch.value) {
                   doSearch(inputSearch.value, query);
                }

                // Or update the query
                else {
                     doUpdateQuery(query.q, query);
                }
            }

            $('#' + config.tabs + ' a').removeClass('active');
            $('#' + config.tabs + ' a.' + query.id).toggleClass('active');

        }
    }

    function initYase() {

        const requestConfig = {
            endpoint: defaultEndpoint || endpoints.length && endpoints[0] || undefined
            // headers: () => ({
            //     //'token': '' // TODO add authenticated token
            // })
        };

        if (!yasqe) {
            const element = document.getElementById(config.yasqe);
            if (!element) throw new Error('Cannot find div with id=' + config.yasqe);

            yasqe = new Yasqe(element, {
                requestConfig
            });

            // Link editor to query
            yasqe.on("queryResponse", receivedResponse);

        }

    }

    function initYasr() {
        const prefixes = yasqe.getPrefixesFromQuery();
        if (!yasr) {
            const element = document.getElementById(config.yasr);
            if (!element) throw new Error('Cannot find div with id=' + config.yasr);


            Yasr.registerPlugin("taxon", YasrTaxonPlugin);

            yasr = new Yasr(element, {
                pluginOrder: ["taxon", "table", "response"],
                prefixes
            });

        }
        else {
            yasr.config.prefixes = prefixes;
        }

    }

    function showDebug(enable) {
        if (enable) {
            $('.debug').removeClass('d-none');
        }
        else {
            $('.debug').addClass('d-none');
        }
    }

    /* -- Search -- */


    function doSearch(searchText, options)
    {
        searchText = searchText || inputSearch.value;
        if (!searchText) return; // Skip if empty

        hideResult();
        showLoading();

        try {
            log("SEARCH: " + searchText, "text-muted");

            // Compute the query
            doUpdateQuery(searchText, options);

            setTimeout(() => {
                yasqe.queryBtn.click();
            })

        }
        catch(error) {
            console.error(error);
            onError({data: (error && error.message || error)});
            hideLoading();
        }
    }


    function doUpdateQuery(searchText, options)
    {

        options = options || {};
        searchText = searchText || inputSearch.value;
        if (!searchText) return; // Skip if empty

        let searchTerms = [searchText];
        if (searchText.indexOf('"') !== -1 || searchText.indexOf(',') !== -1) {
            searchTerms = searchText.split(/[",]+/).map(s => s.trim()).filter(s => s.length > 0);
            console.info("Multiple search:", searchTerms);
        }

        // Auto set exact match, if not set yet
        if (config.exactMatch === undefined) {
            config.exactMatch = (searchText.indexOf('*') === -1);
        }

        // Auto select example
        if (selectedQueryIndex === -1) {
            const autoSelectExampleIndex = queries.map((example, index) => {
                const count = searchTerms.reduce((count, searchTerm) =>
                        ((example.canHandleTerm && example.canHandleTerm(searchTerm)) ? count + 1 : count)
                    , 0);
                return {index,count};
            })
                .sort((e1, e2) => e1.count === e2.count ? 0 : (e1.count > e2.count ? -1 : 1))
                // Take the example that match must of search terms
                .map(e => e.index)[0];
            console.info("Auto select tab index:", autoSelectExampleIndex);
            selectQuery(autoSelectExampleIndex, {silent: true});
        }


        options = {
            limit: config.limit || 50,
            exactMatch: config.exactMatch,
            // Override from the selected query
            ...queries[selectedQueryIndex],
            // Override using given options
            ...options
        };
        options.q = undefined;

        try {
            initYase();

            let binding = {
                ...options.binding,
                limit: config.limit
            };

            let nbLoop = 0;
            let queryString = (options.query || defaultQuery);

            while (queryString.indexOf('{{') !== -1 && nbLoop < 10) {
                queryString = searchTerms.reduce((query, q) => {

                    // Create filter clause
                    const filterClause = (options.filters || [])
                        .map(key => {
                            // If exactMatch, replace 'prefixMatch' with 'exactMatch'
                            if (options.exactMatch) {
                                if (key === 'prefixMatch') return 'exactMatch';
                                if (key === 'codePrefixMatch') return 'codeExactMatch';
                            }
                            return key
                        })
                        .map(key => filtersMap[key])
                        .join('\n\t&& ');

                    // Compute the query
                    query = query.replace('#filter', '\n\t|| (' + filterClause + ') #filter')
                        .replace('{{filter}}', '(' + filterClause + ') #filter');

                    // Replace wildcards by regexp, if NOT exact match
                    binding.q = options.exactMatch ? q : q.replace(/[*]+/g, '.*');
                    binding.defaultPrefix = prefixDefs[0].prefix;

                    // Bind params
                    return Object.keys(binding).reduce((query, key) => {
                        return query.replace('{{' + key + '}}', binding[key])
                    }, query);
                }, queryString);

                nbLoop++;
            }

            // Compute the query
            queryString = queryString.replace('#filter', '');

            yasqe.setValue(queryString);
            log("QUERY: " + queryString);

            // Add prefixes
            const prefixes = (options.prefixes || [])
                .map(p => p === 'this' ? prefixDefs[0].prefix : p) // Replace 'this' by default prefic
                .reduce((res, prefix) => {
                const def = prefixDefs.find(def => def.prefix === prefix);
                res[prefix] = def && def.namespace || undefined;
                return res;
            }, {});
            yasqe.addPrefixes(prefixes);
        }
        catch(error) {
            console.error(error);
            onError({data: (error && error.message || error)});
        }
    }

    function showLoading(enable) {
        // Show
        if (enable !== false) {
            $('#loading').removeClass('d-none');
        }
        // Hide
        else {
            $('#loading').addClass('d-none');
        }
    }

    function hideLoading() {
        showLoading(false);
    }

    function receivedResponse(yasqe, req, duration) {
        log('RESPONSE: received in ' + duration + 'ms');

        initYasr();

        const yasrPlugin = queries[selectedQueryIndex] && queries[selectedQueryIndex].yasrPlugin;
        if (yasrPlugin) yasr.selectPlugin(yasrPlugin);

        yasr.setResponse(req);

        hideLoading();
        showResult();
    }

    function showResult(enable) {
        // Show
        if (enable !== false) {
            $('#' + config.yasr).removeClass('d-none');

            $('#' + config.tabs).removeClass('d-none');
            $('#' + config.optionsDiv).removeClass('d-none'); // Do once

            $('#' + config.optionsDiv + ' #exactMatch').prop("checked", config.exactMatch !== false);
        }
        // Hide
        else {
            $('#' + config.yasr).addClass('d-none');

            // DO NOT hide tabs, nor options
            //$('#' + config.tabs).addClass('d-none');
            //$('#' + config.optionsDiv).addClass('d-none');
        }
    }

    function hideResult() {
        showResult(false);
    }

    window.addEventListener("load", init, false);

    return {
        selectQuery,
        showDebug,
        setConfig,
        clearLog,
        drawTabs,
        doSearch,
    }
}
