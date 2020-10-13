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


function AppDepartmentSearch(config) {
    const defaultConfig = {
        ids: {
            yasqe: 'yasqe',
            yasr: 'yasr',
            tabs: 'tabs',
            options: 'options',
            details: 'details'
        },
        onUriClick: undefined,
        onUriClickTarget: undefined,
        exactMatch: false,
        limit: 50,
        prefix: 'this'
    };

    const endpointsById = {
        THIS: 'this',
        EAU_FRANCE: 'http://id.eaufrance.fr/sparql'
    };

    const NUMERICAL_CODE_REGEXP = new RegExp(/^[0-9]+$/);
    const SCIENTIFIC_NAME_REGEXP = new RegExp(/^[a-zA-Z ]+$/);

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
        rdfsLabel: '?sourceUriUri rdfs:label ?label .',
        dwcScientificName: '?sourceUriUri dwc:scientificName ?scientificName .',

        // Regex filter, on scientific name
        exactMatch: '?{{searchField}}="{{q}}"',
        prefixMatch: 'regex( ?{{searchField}}, "^{{q}}", "i" )',
        anyMatch: 'regex( ?{{searchField}}, "{{q}}", "i" )',

        // Regex filter, on uri (code)
        codeExactMatch: 'strEnds( str(?sourceUri), "/{{q}}" )',
        codePrefixMatch: 'regex( str(?sourceUri), "/{{q}}$", "i" )'
    };
    const queries = [
        {
            id: 'local-name',
            name: 'Search by name',
            canHandleTerm: (term) => term && term.trim().match(/^[A-Za-z ]+$/),
            yasrPlugin : 'department',
            q: 'Environnement',
            prefixes: ['dc', 'dcterms', 'rdf', 'rdfs', 'owl', 'skos', 'foaf', 'org', 'gr', 's', 'inc1', 'inc', 'incdata', 'this'],
            query: "PREFIX siret: <https://api.insee.fr/entreprises/sirene/V3/siret/>\n" +
                "SELECT DISTINCT \n" +
                "  ?sourceUri ?label ?address ?parent \n" +
                "  ?exactMatch ?seeAlso ?created ?modified \n" +
                "WHERE {\n" +
                "  ?sourceUri rdfs:label ?label ;\n" +
                "       rdf:type ?type ;\n" +
                "  OPTIONAL {\n" +
                "    ?sourceUri org:hasprimarySite _:site .\n" +
                "    _:site org:siteAddress ?address .\n" +
                "  }\n" +
                "  FILTER (\n" +
                "     ({{filter}})\n" +
                "     && (?type = org:Organization || ?type = {{defaultPrefix}}:Department) \n" +
                "  ) .\n" +
                "  OPTIONAL {\n" +
                "    ?sourceUri skos:exactMatch|owl:sameAs ?exactMatch .\n" +
                "  }\n" +
                "  OPTIONAL {\n" +
                "    ?sourceUri rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .\n" +
                "  }\n" +
                "  OPTIONAL {\n" +
                "    ?sourceUri skos:broader ?parent .\n" +
                "  }\n" +
                "  OPTIONAL {\n" +
                "    ?sourceUri dc:created|dcterms:created ?created ;\n" +
                "      dc:modified|dcterms:modified ?modified .\n" +
                "  }\n" +
                "} LIMIT {{limit}}",
            filters: ['anyMatch'],
            binding: {
                searchField: 'label'
            }
        },
        {
            id: 'local-city',
            name: 'Search by city',
            canHandleTerm: (term) => term && term.trim().match(/^[A-Za-z ]+$/),
            yasrPlugin : 'department',
            q: 'Environnement',
            prefixes: ['dc', 'rdf', 'rdfs', 'owl', 'skos', 'foaf', 'org', 'gr', 's', 'inc1', 'inc', 'incdata', 'this'],
            query: "PREFIX siret: <https://api.insee.fr/entreprises/sirene/V3/siret/>\n" +
                "SELECT DISTINCT \n" +
                "  ?sourceUri ?label ?address ?parent \n" +
                "  ?exactMatch ?seeAlso ?created ?modified \n" +
                "WHERE {\n" +
                "  ?sourceUri rdfs:label ?label ;\n" +
                "       rdf:type ?type ;\n" +
                "       org:hasprimarySite _:site .\n" +
                "  _:site org:siteAddress ?address ;\n" +
                "         s:addressLocality ?city ." +
                "  FILTER (\n" +
                "     ({{filter}})\n" +
                "     && (?type = org:Organization || ?type = {{defaultPrefix}}:Department) \n" +
                "  ) .\n" +
                "  OPTIONAL {\n" +
                "    ?sourceUri skos:exactMatch|owl:sameAs ?exactMatch .\n" +
                "  }\n" +
                "  OPTIONAL {\n" +
                "    ?sourceUri rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .\n" +
                "  }\n" +
                "  OPTIONAL {\n" +
                "    ?sourceUri skos:broader ?parent .\n" +
                "  }\n" +
                "  OPTIONAL {\n" +
                "    ?sourceUri dc:created ?created ;\n" +
                "      dc:modified ?modified .\n" +
                "  }\n" +
                "} LIMIT {{limit}}",
            filters: ['prefixMatch'],
            binding: {
                searchField: 'city'
            }
        },
        {
            id: 'remote-name',
            name: 'Federated search by name',
            canHandleTerm: (term) => term && term.trim().match(/^[A-Za-z ]+$/),
            yasrPlugin : 'department',
            debug: false,
            q: 'Environnement',
            prefixes: ['dc', 'dcterms', 'rdf', 'rdfs', 'owl', 'skos', 'foaf', 'org', 'gr', 's', 'inc1', 'inc', 'incdata', 'this'],
            query: 'PREFIX siret: <https://api.insee.fr/entreprises/sirene/V3/siret/>\n' +
                'SELECT DISTINCT \n' +
                '  ?sourceUri ?label ?address ?parent \n' +
                '  ?created ?modified \n' +
                '  ?exactMatch ?seeAlso \n' +
                'WHERE {\n' +

                // -- Sandre endpoint part
                ' { SERVICE <{{eauFranceEndpoint}}> {\n' +
                '    ?sourceUri rdfs:label ?label ;\n' +
                '      rdf:type inc:Interlocuteur ;\n' +
                '    FILTER(\n' +
                '      ({{filter}})\n' +
                '    )\n' +
                '    OPTIONAL {\n' +
                '      ?sourceUri skos:exactMatch|owl:sameAs ?exactMatch .\n' +
                '      #FILTER ( isURI(?exactMatch) )\n' +
                '    }\n' +
                'OPTIONAL {\n' +
                '    ?sourceUri inc1:AdresseInterlocuteur ?bnAddress .\n' +
                '    OPTIONAL { ?bnAddress inc1:Compl2Adresse ?addressCompl2 . }\n' +
                '    OPTIONAL { ?bnAddress inc1:Compl3Adresse ?addressCompl3 . }\n' +
                '    OPTIONAL { ?bnAddress inc1:NumLbVoieAdresse ?addressRoad . }\n' +
                '    OPTIONAL { ?bnAddress inc1:LgAcheAdresse ?postalCodeAndCity . }\n' +
                '    BIND(\n' +
                '      REPLACE(REPLACE(\n' +
                '        CONCAT(?addressCompl2, \'|\', ?addressCompl3, \'|\', ?addressRoad),\n' +
                '          \'^[|]+\', \'\', \'i\'),\n' +
                '        \'[|]+\', \', \', \'i\')\n' +
                '      as ?streetAddress\n' +
                '    )\n' +
                '    BIND(\n' +
                '      REPLACE(REPLACE(\n' +
                '        CONCAT(?streetAddress, \'|\', ?postalCodeAndCity),\n' +
                '          \'^[|]+\', \'\', \'i\'),\n' +
                '        \'[|]+\', \', \', \'i\')\n' +
                '      as ?address\n' +
                '    )\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri inc1:PaysInterlocuteur _:country .\n' +
                '    _:country inc1:CdPays ?addressCountry .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .\n' +
                '    #FILTER ( isURI(?seeAlso) )\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?sourceUri dc:created|inc1:DateCreInterlocuteur ?created ;\n' +
                '      dc:modified|inc1:DateMAJInterlocuteur ?modified .\n' +
                '  }\n' +
                ' }}\n' +
                '} LIMIT {{limit}}',
            filters: ['anyMatch'],
            binding: {
                eauFranceEndpoint: endpointsById.EAU_FRANCE,
                searchField: 'label'
            }
        }
     ];

    // SparQL var
    let defaultEndpoint,
        defaultPrefixUri,
        endpoints;

    // Form elements
    let output,
        inputSearch,
        details,
        selectedQueryIndex = -1;

    // YasGui
    let yasqe, yasr, onYasqeQueryResponse;

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
        console.debug("Init department search app...");

        config = {
            ...defaultConfig,
            ...config
        };

        if (window.location && window.location.origin) {

            // Compute default endpoint
            defaultEndpoint = window.location.origin + '/sparql';

            endpointsById.THIS = defaultEndpoint;

            // Update the default prefix
            defaultPrefixUri = window.location.origin + '/ontology/data/Department/';
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
        output = document.getElementById("output");

        // Collect endpoints, from default, endpoint map, and queries
        endpoints = Object.keys(endpointsById).map(key => endpointsById[key])
            .reduce((res, ep) => (!ep || res.findIndex(endpoint => endpoint === ep) !== -1) ? res : res.concat(ep),
                [defaultEndpoint]);

        details = document.getElementById(config.ids.details);

        initDropzone();

        // Add tabs
        drawTabs();
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

        elementId = elementId || config.ids.tabs;
        config.ids.tabs = elementId;

        // Queries
        const tabs = queries.map((example, index) => {
            if (!example.id) return ''; // Skip if no name (e.g. default)
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

            $('#' + config.ids.tabs + ' a').removeClass('active');
            $('#' + config.ids.tabs + ' a.' + query.id).toggleClass('active');

        }
    }

    function initYase(opts) {
        opts = opts || {};

        const requestConfig = {
            endpoint: defaultEndpoint || endpoints.length && endpoints[0] || undefined
            // headers: () => ({
            //     //'token': '' // TODO add authenticated token
            // })
        };

        if (!yasqe) {
            const element = document.getElementById(config.ids.yasqe);
            if (!element) throw new Error('Cannot find div with id=' + config.ids.yasqe);

            yasqe = new Yasqe(element, {
                requestConfig
            });

            // Listen query response
            yasqe.on("queryResponse", (yasqe, res, duration) => {
                if (onYasqeQueryResponse) return onYasqeQueryResponse(yasqe, res, duration)
            });
        }

        // Apply the correct query response function
        onYasqeQueryResponse = opts.queryResponse || displayResponse;
    }

    function initYasr() {
        const prefixes = yasqe.getPrefixesFromQuery();
        if (!yasr) {
            const element = document.getElementById(config.ids.yasr);
            if (!element) throw new Error('Cannot find div with id=' + config.ids.yasr);


            Yasr.registerPlugin("department", YasrDepartmentPlugin);
            Yasr.registerPlugin("taxon", YasrDepartmentPlugin);

            YasrDepartmentPlugin.prototype.defaults.onUriClick = config.onUriClick;
            YasrDepartmentPlugin.prototype.defaults.onUriClickTarget = config.onUriClickTarget;

            yasr = new Yasr(element, {
                pluginOrder: ["department", "table", "response"],
                prefixes
            });

        }
        else {
            yasr.config.prefixes = prefixes;
        }

    }

    function initDropzone() {
        // Get the template HTML and remove it from the doumenthe template HTML and remove it from the doument
        const previewNode = document.querySelector("#template");
        previewNode.id = "";
        const previewTemplate = previewNode.parentNode.innerHTML;
        previewNode.parentNode.removeChild(previewNode);

        const myDropzone = new Dropzone('.container', // Make the whole body a dropzone
            {
                url: "/api/department/search",
                parallelUploads: 20,
                previewTemplate: previewTemplate,
                autoQueue: false, // Make sure the files aren't queued until manually added
                previewsContainer: "#previews", // Define the container to display the previews
                clickable: ".fileinput-button" // Define the element that should be used as click trigger to select files.
            });

        myDropzone.on("addedfile", function(file) {
            $('#previews').removeClass('d-none');

            let importClick;
            // If CSV file: parse it
            if (file.type === "text/csv" || file.type === "text/plain"
                || file.type === "application/vnd.ms-excel" // on MS Windows
            ) {
                queries.forEach((query, index) => {
                    const button = file.previewElement.querySelector(".import-" + query.id);
                    if (button) {
                        button.onclick = () => importFile(file, {queryIndex: index});
                    }
                })
            }

            // Upload to server
            else {
                // TODO: test this !
                file.previewElement.querySelector(".import").onclick = () => myDropzone.enqueueFile(file)
            }
        });

        myDropzone.on("sending", function(file) {
            // Disable import button
            file.previewElement.querySelector(".import").setAttribute("disabled", "disabled");
        });

        myDropzone.on("success", function(file) {
            console.log("[department-search] TODO: success upload ! ", file)
        });


        // Hide the total progress bar when nothing's uploading anymore
        myDropzone.on("queuecomplete", function(progress) {
            console.log("[department-search] TODO queuecomplete!", progress)
        });

        // Hide the total progress bar when nothing's uploading anymore
        myDropzone.on("removedfile", function(file) {
            console.debug("[department-search] Removing file '{0}'".format(file.name));
            file.status = 'cancelled';
        });

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

    function doSubmit(event) {
        if (event) {
            if (event.defaultPrevented) return;
            event.stopPropagation();
            event.preventDefault();
        }
        doSearch();
        return false;
    }

    function doSearch(searchText, opts)
    {
        searchText = searchText || inputSearch.value;
        if (!searchText || searchText.trim().length === 0) return; // Skip if empty
        searchText = searchText.trim();
        console.debug("Search: " + searchText)

        hideResult();
        hideFilePreviews();

        showLoading();

        try {
            log("SEARCH: " + searchText, "text-muted");

            // Compute the query
            doUpdateQuery(searchText, opts);

            runQuery();

        }
        catch(error) {
            console.error(error);
            onError({data: (error && error.message || error)});
            hideLoading();
        }
    }

    function runQuery() {
        setTimeout(() => {
            yasqe.queryBtn.click();
        })
    }

    function searchAsPromise(searchText, opts) {

        return new Promise((resolve, reject) => {
            doUpdateQuery(searchText, {
                ...opts,
                queryResponse: (yasqe, res, duration) => {
                    if (res.type !== 'application/sparql-results+json') {
                        reject("Response content type '{}' not implemented yet".format(res.type));
                    }

                    console.debug("[department-search] Search on '{0}' give {1} results:".format(searchText, res.body && res.body.results && res.body.results.bindings.length || 0));
                    resolve(res);
                }
            })
            setTimeout(() => {
                yasqe.queryBtn.click();
            })
        });
    }

    function doUpdateQuery(searchText, opts)
    {

        opts = opts || {};

        opts.queryIndex = opts.queryIndex >= 0 ? opts.queryIndex : selectedQueryIndex;

        searchText = searchText || inputSearch.value;
        if (!searchText) return; // Skip if empty

        let searchTerms;
        if (typeof searchText === "string") {
            searchTerms = searchText.split(/[",;+\t]+/).map(s => s.trim()).filter(s => s.length > 0);
        }
        else if (typeof searchText === "array") {
            searchTerms = searchText.map(s => s.trim()).filter(s => s.length > 0);
        }
        else {
            throw new Error("Invalid argument: " + searchText);
        }
        if (searchTerms.length > 1) {
            console.info("Multiple search:", searchTerms);
        }

        // Auto set exact match, if not set yet
        if (config.exactMatch === undefined) {
            config.exactMatch = (searchText.indexOf('*') === -1);
        }

        // Auto select example
        if (opts.queryIndex === -1) {
            opts.queryIndex = queries.map((example, index) => {
                const count = searchTerms.reduce((count, searchTerm) =>
                        ((example.canHandleTerm && example.canHandleTerm(searchTerm)) ? count + 1 : count)
                    , 0);
                return {index,count};
            })
                .sort((e1, e2) => e1.count === e2.count ? 0 : (e1.count > e2.count ? -1 : 1))
                // Take the example that match must of search terms
                .map(e => e.index)[0];
            console.info("Auto select tab index:", opts.queryIndex);
        }
        if (opts.queryIndex !== selectedQueryIndex) {
            selectQuery(opts.queryIndex, {silent: true});
        }

        opts = {
            limit: config.limit || 50,
            exactMatch: config.exactMatch,
            // Override from the selected query
            ...queries[opts.queryIndex],
            // Override using given options
            ...opts
        };
        opts.q = undefined;

        try {
            initYase({
                queryResponse: opts.queryResponse
            });

            let binding = {
                ...opts.binding,
                limit: config.limit
            };

            let nbLoop = 0;
            let queryString = (opts.query || defaultQuery);

            while (queryString.indexOf('{{') !== -1 && nbLoop < 10) {
                queryString = searchTerms.reduce((query, q) => {

                    // Create filter clause
                    const filterClause = (opts.filters || [])
                        .map(key => {
                            // If exactMatch, replace 'prefixMatch' with 'exactMatch'
                            if (opts.exactMatch) {
                                if (key === 'prefixMatch' || key === 'anyMatch') return 'exactMatch';
                                if (key === 'cityPrefixMatch' || key === 'cityAnyMatch') return 'cityExactMatch';
                                if (key === 'codePrefixMatch') return 'codeExactMatch';
                            }
                            return key
                        })
                        .map(key => filtersMap[key])
                        .join('\n\t&& ');

                    // Compute the query
                    query = query.replace('#filter', '\n\t|| (' + filterClause + ') #filter\n\t')
                        .replace('{{filter}}', '(' + filterClause + ') #filter\n\t');

                    // Replace wildcards by regexp, if NOT exact match
                    binding.q = opts.exactMatch ? q : q.replace(/[*]+/g, '.*');
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
            const prefixes = (opts.prefixes || [])
                .map(p => p === 'this' ? prefixDefs[0].prefix : p) // Replace 'this' by default prefix
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

    function displayResponse(yasqe, response, duration) {
        log('RESPONSE: received in ' + duration + 'ms');

        initYasr();

        const yasrPlugin = queries[selectedQueryIndex] && queries[selectedQueryIndex].yasrPlugin;
        if (yasrPlugin) yasr.selectPlugin(yasrPlugin);

        yasr.setResponse(response);

        hideLoading();
        showResult();
    }

    function showResult(enable) {
        // Show
        if (enable !== false) {
            $('#' + config.ids.yasr).removeClass('d-none');

            $('#' + config.ids.tabs).removeClass('d-none');
            $('#' + config.ids.options).removeClass('d-none'); // Do once

            $('#' + config.ids.options + ' #exactMatch').prop("checked", config.exactMatch !== false);

            hideDetails();
        }
        // Hide
        else {
            $('#' + config.ids.yasr).addClass('d-none');

            // DO NOT hide tabs, nor options
            //$('#' + config.ids.tabs).addClass('d-none');
            //$('#' + config.ids.options).addClass('d-none');
        }
    }

    function hideResult() {
        showResult(false);
    }

    function showDetails(url) {

        const detailsContainer = $('#' + config.ids.details);
        const iframe = $('#' + config.ids.details + " iframe");

        // Show
        if (url) {
            // Hide iframe
            iframe.addClass('d-none');

            // Show the details loading spinner
            detailsContainer.addClass('loading').removeClass('d-none');

            // Change the iframe src attribute
            iframe.attr('src', url);
            iframe.on('load', function() {
                console.debug("Iframe loaded !", arguments);

                // Hide loading spinner
                detailsContainer.removeClass('loading');

                // Show iframe
                iframe.removeClass('d-none');

            });
        }

        // Hide
        else {
            detailsContainer.addClass('d-none').removeClass('loading');

            // Hide iframe
            iframe.addClass('d-none');

            // Remove iframe content
            iframe.attr('src', undefined);
        }
    }

    function hideDetails() {
        showDetails(false);
    }

    function showFilePreviews(enable) {
        if (enable !== false) {
            $('#previews').removeClass('d-none');
        }
        else {
            $('#previews').addClass('d-none');
        }
    }

    function hideFilePreviews() {
        showFilePreviews(false);
    }

    async function importFile(file, opts) {
        opts = opts || {};
        if (opts.queryIndex < 0) return; // Skip

        const query = queries[opts.queryIndex];
        const queryIdParts = query.id && query.id.split('-');
        const valueType = queryIdParts && queryIdParts[queryIdParts.length - 1].toLowerCase() || 'code';

        let lineFilter = query.canHandleTerm;
        if (!lineFilter) {
            let lineFilterRegexp;
            switch (valueType) {
                case 'name':
                    lineFilterRegexp = SCIENTIFIC_NAME_REGEXP;
                    break;
                case 'code':
                case 'aphiaid':
                    lineFilterRegexp = NUMERICAL_CODE_REGEXP;
                    break;
                default: throw new Error('Invalid value type: ' + opts.valueType);
            }
            lineFilter = (v) => lineFilterRegexp.test(v);
        }

        const now = Date.now();
        console.info("[department-search] Importing file '{0}'...".format(file.name));

        const setProgression = getFileProgressionFn(file);

        let progression = 0;
        setProgression(progression);
        hideFileResult(file);

        const values = await readFileAsLines(file);

        const validValues = values.filter(lineFilter);

        // Update progression
        progression += 10;
        setProgression(progression);

        console.info("[department-search] Found {0} valid {1}s".format(validValues.length, opts.valueType))
        if (!values.length) {
            setProgression(100);
            return; // Nothing to search
        }

        // Compute progression steps
        const progressionStep = Math.max(Math.trunc((90 / validValues.length) * 10) / 10, 0.1);

        // Chain search on each value
        let res,
            lookupVar = 'lookup',
            counter = 0,
            matchCount = 0;
        for (value of validValues) {

            // Check if cancelled
            if (file.status === 'cancelled') {
                hideResult();
                return; // Stop
            }

            // Search on value
            const curRes = await searchAsPromise(value, opts);

            const hasResult = curRes.body && curRes.body.results && curRes.body.results.bindings && curRes.body.results.bindings.length > 0;

            // If first response, init the result
            if (!res) {
                // Copy the first result, to init final response
                res = {...curRes,
                    body: {
                        head: {
                            // Add lookup var
                            vars: [lookupVar].concat(curRes.body.head.vars)
                        },
                        results: {
                            // Reset bindings (will be add later)
                            bindings: []
                        }
                    }
                };
            }

            let bindings;
            if (hasResult) {
                bindings = curRes.body.results.bindings;

                // Add the lookup value to each bindings
                bindings.forEach(binding => {
                    binding[lookupVar] = {value, type: 'literal', found: true};
                });
            }
            else {
                // Create a fake binding, for the missing value
                const binding = {};
                binding[lookupVar] = {value, type: 'literal', found: false};
                switch(valueType.toLowerCase()) {
                    case 'code':
                        binding.sourceUri = {value, type: 'literal'};
                        break;
                    case 'name':
                        binding.scientificName = {value, type: 'literal'};
                        break;
                    case 'aphiaid':
                        binding.exactMatch = {value: 'urn:lsid:marinespecies.org:taxname:' + value, type: 'uri'};
                        break;
                    default:
                        console.warn('Unknown value type: ' + valueType);
                }

                bindings = [binding];
            }

            // Append final bindings
            res.body.results.bindings = res.body.results.bindings.concat(bindings);

            // Update counter
            matchCount += hasResult ? 1 : 0;

            // Update progression
            progression += progressionStep;
            setProgression(progression);

            // Display result, every 5 value
            if (progression < 100 && counter % 5 === 0) {
                displayResponse(this.yasgui, res, 0);
            }
        }

        // All search has been executed
        setProgression(100);
        const duration = Date.now() - now;
        console.debug("[department-search] All {0} imported in {1} ms".format(valueType, duration));

        if (res) {
            file.response = res;
            file.duration = duration;

            displayFileResponse(file);
        }
        else {
            hideResult();
        }

        displayFileExecutionResult(file, {
            totalCount: values.length,
            matchCount: matchCount,
            ignoreCount : values.length - validValues.length,
            missingCount: (validValues.length - matchCount)
        });
    }

    function readFileAsLines(file, opts) {
        opts = opts || {};
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = function(onLoadEvent) {
                if (!onLoadEvent.loaded || !reader.result) {
                    reject('Unable to parse file !');
                    return;
                }

                const values = reader.result.split("\n")
                    .map(value => value && value.trim())
                    .filter(value => value && value.length > 0 && (!opts.filter || opts.filter(value)));

                resolve(values);
            }
            reader.readAsText(file);
        })
    }

    function displayFileExecutionResult(file, {totalCount, matchCount, ignoreCount, missingCount}) {
        if (!file || !file.previewElement) return;
        const resultElement = file.previewElement.querySelector(".result");
        if (resultElement) {
            // Total element
            {
                const totalEl = document.createElement("a");
                totalEl.setAttribute('href', '#');
                totalEl.innerText = totalCount + ' row' + (totalCount > 1 ? 's' : '');
                totalEl.onclick = () => displayFileResponse(file);
                resultElement.appendChild(totalEl);
            }

            {
                const span = resultElement.appendChild(document.createElement("span"));
                span.innerText = ' (';
                resultElement.appendChild(span);
            }

            // Ignore count
            if (ignoreCount > 0) {
                const ignoreEl = resultElement.appendChild(document.createElement("span"));
                ignoreEl.innerText = ignoreCount + ' ignored';
                resultElement.appendChild(ignoreEl);
            }

            // Match count
            if (matchCount > 0) {
                if (ignoreCount > 0) {
                    const span = resultElement.appendChild(document.createElement("span"));
                    span.innerText = ', ';
                    resultElement.appendChild(span);
                }
                const matchEl = document.createElement("a");
                matchEl.setAttribute('href', '#');
                matchEl.innerText = matchCount + ' found';
                matchEl.onclick = () => displayFileResponse(file, {valid: true});
                resultElement.appendChild(matchEl);
            }

            // Not found count
            if (missingCount > 0) {
                if (ignoreCount > 0 || matchCount > 0) {
                    const span = resultElement.appendChild(document.createElement("span"));
                    span.innerText = ', ';
                    resultElement.appendChild(span);
                }
                const missingEl = document.createElement("a");
                missingEl.setAttribute('href', '#');
                missingEl.innerText = missingCount + ' not found';
                missingEl.onclick = () => displayFileResponse(file, {valid: false});
                resultElement.appendChild(missingEl);
            }

            {
                const span = resultElement.appendChild(document.createElement("span"));
                span.innerHTML = ')';
                resultElement.appendChild(span);
            }
        }

        // Update buttons
        if (missingCount > 0) {
            const createMissingButton = file.previewElement.querySelector(".btn.create-missing");
            if (createMissingButton) {
                $('#create-missing-toast').toast({autohide: true, delay: 2500});
                createMissingButton.classList.remove('d-none');
                createMissingButton.onclick = () => {
                    console.debug('[department-search] Asking creation of missing references...');
                    $('#create-missing-toast').toast('show');
                }
            }
        }

        // Hide the import button
        //const importButton = file.previewElement.querySelector(".import-dropdown");
        //if (importButton) {
        //    importButton.classList.add('d-none')
        //}
    }

    function hideFileResult(file) {
        if (!file || !file.previewElement) return;
        const el = file.previewElement.querySelector(".result");
        if (!el) return;
        el.innerHTML = '';
    }

    function displayFileResponse(file, opts) {
        if (!file || !file.response) return; // Skip

        // Create a filter function, from options
        let filterFn;
        if (opts && opts.valid === true) {
            filterFn = (b) => !!b.sourceUri;
        }
        else if (opts && opts.valid === false) {
            filterFn = (b) => !b.sourceUri;
        }

        if (filterFn) {
            const response = {
                ...file.response,
                body: {
                    head: file.response.body.head,
                    results: {
                        bindings: file.response.body.results.bindings.filter(b => !filterFn || filterFn(b))
                    }
                }
            };
            displayResponse(this.yasqe, response, file.duration);
        }
        else {
            displayResponse(this.yasqe, file.response, file.duration);
        }

    }

    function getFileProgressionFn(file) {
        const progressElement = file.previewElement.querySelector("[data-dz-uploadprogress]");
        if (!progressElement) throw new Error("Cannot found [data-dz-uploadprogress] in file template !");
        return (value) => {
            progressElement.setAttribute('style', 'width: '+ Math.min(100, value) +'%');
        }

    }


    window.addEventListener("load", init, false);

    return {
        selectQuery,
        showDebug,
        setConfig,
        clearLog,
        drawTabs,
        doSubmit,
        doSearch,
        showDetails,
        hideDetails
    }
}
