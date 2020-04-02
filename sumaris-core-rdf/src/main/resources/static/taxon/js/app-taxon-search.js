class YasrTaxonPlugin {
    // A priority value. If multiple plugin support rendering of a result, this value is used
    // to select the correct plugin
    priority = 10;

    // Name
    label = "Taxon";

    // Whether to show a select-button for this plugin
    hideFromSelection = false;

    constructor(yasr) {
        this.yasr = yasr;
    }

    getTaxonsFromBindings(bindings) {
        const taxonsByUri = {};
        bindings.forEach(binding => {
            const uri = binding.subject.value;
            if (!taxonsByUri[uri]) {
                taxonsByUri[uri] = {
                    uri,
                    scientificName: binding.scientificName.value,
                    seeAlso : [],
                    exactMatch : []
                }
            }
            if (binding.exactMatch && binding.exactMatch.value && !taxonsByUri[uri].exactMatch.includes(binding.exactMatch.value)) {
                taxonsByUri[uri].exactMatch.push(binding.exactMatch.value)
            }
            if (binding.seeAlso && binding.seeAlso.value && !taxonsByUri[uri].seeAlso.includes(binding.seeAlso.value)) {
                taxonsByUri[uri].seeAlso.push(binding.seeAlso.value)
            }
        });
        return Object.keys(taxonsByUri).map(key => taxonsByUri[key]).sort((t1, t2) => t1.scientificName === t2.scientificName ? 0 : (t1.scientificName > t2.scientificName ? 1 : -1));
    }

    // Draw the resultset. This plugin simply draws the string 'True' or 'False'
    draw() {
        const el = document.createElement("div");

        // Get taxon list
        const taxons = this.getTaxonsFromBindings(this.yasr.results.json.results.bindings);

        el.innerHTML = "<table class='table table-striped'>" +
            "<thead>" +
            " <tr>" +
            "  <th scope='col'>#</th>" +
            "  <th scope='col'>Scientific name</th>" +
            "  <th scope='col'>Source</th>" +
            "  <th scope='col'>Exact match / seeAlso</th>" +
            " </tr>" +
            "</thead>" +
            taxons.map((taxon, index) => {
                return "<tr>" +
                    " <th scope=\"row\">" + (index+1) + "</th>" +
                    " <td class='col col-50'>" + taxon.scientificName + "</td>" +
                    " <td>" +
                    "  <a href='"+ taxon.uri +"'>"+taxon.uri+"</a>" +
                    " </td>" +
                    " <td>" +
                    taxon.exactMatch.concat(taxon.seeAlso).map(uri => {
                        return "  <a href='"+ uri +"'>"+uri+"</a>"
                    }).join("<br>") +
                    " </td>" +
                    "</tr>";
            }).join('\n') + "</table>";
        this.yasr.resultsEl.appendChild(el);
    }

    // A required function, used to indicate whether this plugin can draw the current
    // resultset from yasr
    canHandleResults() {
        return (
            this.yasr.results.type === 'json' && this.yasr.results.json.head
            && this.yasr.results.json.head.vars.includes('scientificName')
            && this.yasr.results.json.head.vars.includes('subject')
        );
    }
    // A required function, used to identify the plugin, works best with an svg
    getIcon() {
        const textIcon = document.createElement("div");
        textIcon.classList.add("plugin_icon");
        textIcon.innerText = "✓";
        return textIcon;
    }
}

function AppTaxonSearch(config) {
    const defaultOptions = {
        yasqe: 'yasqe',
        yasqr: 'yasqr',
        examplesDiv: 'examples'
    };

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


    const defaultQuery = "SELECT DISTINCT * WHERE {\n" +
        "  {{where}} \n" +
        "} LIMIT {{limit}}";

    const filtersMap = {
        // Schema filter
        rdfType: 'rdf:type {{rdfType}}',
        rdfsLabel: '?subject rdfs:label ?scientificName .',
        dwcScientificName: '?subject dwc:scientificName ?scientificName .',

        // Regex filter
        exactMatch: '?scientificName="{{q}}"',
        prefixMatch: 'regex( ?scientificName, "^{{q}}", "i" )',
        anyMatch: 'regex( ?scientificName, "{{q}}", "i" )',
        codeMatch: 'regex( str(?subject), "/{{q}}$", "i" )'
    };
    const examples = [
        {
            id: 'local-name',
            name: 'Search on name',
            canHandleTerm: (term) => term && term.trim().match(/^[A-Za-z ]+$/),
            q: 'Lophius budegassa',
            prefixes: ['rdf', 'owl', 'skos', 'foaf', 'dwc', 'dwctn', 'apt', 'rdfs'],
            query: 'SELECT DISTINCT ?scientificName ?subject ?exactMatch ?seeAlso \n' +
                'WHERE {\n' +
                '  ?subject dwc:scientificName ?scientificName ;\n' +
                '       rdf:type dwctn:TaxonName .\n' +
                '  FILTER (\n' +
                '     {{filter}}\n' +
                '  ) .\n' +
                '  OPTIONAL {\n' +
                '    ?subject skos:exactMatch|owl:sameAs ?exactMatch .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?subject rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .\n' +
                '  }\n' +
                '} LIMIT {{limit}}',
            filters: ['prefixMatch'],
            binding: {}
        },

        {
            id: 'local-code',
            name: 'Search on code',
            canHandleTerm: (term) => term && term.trim().match(/^[0-9]+$/),
            q: '847866',
            prefixes: ['rdf', 'owl', 'skos', 'foaf', 'dwc', 'dwctn', 'apt', 'rdfs'],
            query: 'SELECT DISTINCT ?subject ?scientificName ?exactMatch ?seeAlso \n' +
                'WHERE {\n' +
                '  ?subject dwc:scientificName ?scientificName ;\n' +
                '       rdf:type dwctn:TaxonName .\n' +
                '  FILTER (\n' +
                '     {{filter}}\n' +
                '  ) .\n' +
                '  OPTIONAL {\n' +
                '    ?subject skos:exactMatch|owl:sameAs ?exactMatch .\n' +
                '  }\n' +
                '  OPTIONAL {\n' +
                '    ?subject rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso .\n' +
                '  }\n' +
                '} LIMIT {{limit}}',
            filters: ['codeMatch'],
            binding: {
            }
        },

        {
            id: 'remote-name',
            name: 'Remote search',
            canHandleTerm: (term) => term && term.trim().match(/^[A-Za-z ]+$/),
            debug: false,
            q: 'Lophius budegassa',
            prefixes: ['rdf',  'rdfs', 'owl', 'skos', 'foaf', 'dwc', 'dwctn', 'apt'],
            query: 'CONSTRUCT {\n' +
                '  ?subject2 dwc:scientificName ?scientificName2 ;\n' +
                '    rdf:type dwctn:TaxonName ;\n' +
                '    skos:exactMatch ?match2 ;\n' +
                '    rdfs:seeAlso ?seeAlso2 .\n' +
                '  ?subject3 dwc:scientificName ?scientificName3 ;\n' +
                '    rdf:type dwctn:TaxonName ;\n' +
                '    skos:exactMatch ?match3 ;    \n' +
                '    rdfs:seeAlso ?seeAlso3 .\n' +
                '}\n' +
                'WHERE {\n' +
                '  SERVICE <http://taxref.mnhn.fr/sparql> {\n' +
                '    ?subject2 dwc:scientificName ?scientificName2 ; rdf:type dwctn:TaxonName .\t\n' +
                '    FILTER(\n' +
                '       ( ?scientificName2 = "{{q}}" )\n' + // TODO: regexp not supported
                '    )\n' +
                '    OPTIONAL {\n' +
                '      ?subject2 skos:exactMatch|owl:sameAs ?match2 .\n' +
                '    }\n' +
                '    OPTIONAL {\n' +
                '      ?subject2 rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso2 .\n' +
                '    }\n' +
                '  }\n' +
                '  SERVICE <http://id.eaufrance.fr/sparql> {\n' +
                '    ?subject3 dwc:scientificName ?scientificName3 ; rdf:type ?type3 .\t\n' +
                '    FILTER(\n' +
                '      ( ?scientificName3 = "{{q}}"  || regex( str(?scientificName3), "^{{q}}") )\n' +
                '       && ( ?type3 = dwctn:TaxonName || URI(?type3) = apt:AppelTaxon ) \n' +
                '    )\n' +
                '    OPTIONAL {\n' +
                '      ?subject3 skos:exactMatch|owl:sameAs ?match3 .\n' +
                '      FILTER ( isURI(?match3) )\n' +
                '    }\n' +
                '    OPTIONAL {\n' +
                '      ?subject3 rdf:seeAlso|rdfs:seeAlso|foaf:page ?seeAlso3 .\n' +
                '      FILTER ( isURI(?seeAlso3) )\n' +
                '    }\n' +
                '  } \n' +
                '} LIMIT {{limit}}',
            filters: [],
            binding: {}
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
            prefixes: ['rdf', 'dwc',  'dwctn', 'this'],
            query: defaultQuery,
            filters: ['rdfType', 'dwcScientificName', 'prefixMatch'],
            binding: {
                rdfType: 'dwctn:TaxonName'
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
        selectedExampleIndex = -1;

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
            ...defaultOptions,
            ...config
        };

        if (window.location && window.location.origin) {

            // Compute default endpoint
            defaultEndpoint = window.location.origin + '/sparql';

            endpointsById.THIS = defaultEndpoint;

            // Compute default URI
            defaultPrefixUri = window.location.origin + '/ontology/schema/';
            examplePrefixes[0].prefix = defaultPrefixUri;
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

        // Collect endpoints, from default, endpoint map, and examples queries
        endpoints = Object.keys(endpointsById).map(key => endpointsById[key])
            .reduce((res, ep) => (!ep || res.findIndex(endpoint => endpoint === ep) !== -1) ? res : res.concat(ep),
                [defaultEndpoint]);

        app.displayExamples("examples");
    }

    /* -- Examples -- */

    function displayExamples(elementId) {

        elementId = elementId || config.examplesDiv;
        config.examplesDiv = elementId;

        // Queries
        const tabs = examples.map((example, index) => {
            if (!example.id) return res; // Skip if no name (e.g. default)
            const debugClassList = example.debug ? ['debug', 'd-none'].join(' ') : '';
            return '<li class="nav-item "'+ debugClassList + '">' +
                '<a href="#" class="nav-link '+ example.id + ' ' + debugClassList +'"' +
                ' onclick="app.showExample('+index+')">'+example.name+'</a></li>';
        }).join('\n');

        const innerHTML = tabs && "<ul class=\"nav nav-tabs\">" + tabs + "</ul>" || "";

        const element = document.getElementById(elementId);
        element.innerHTML = innerHTML;

    }



    function showExample(index, options) {
        options = options || {};
        if (selectedExampleIndex === index) return; // Skip if same

        const example = examples[index];
        if (example) {
            // Remember this choice
            selectedExampleIndex = index;
            inputSearch.placeholder = example.q;

             // re-run the search
            if (options.silent !== true) {
                if (inputSearch.value) {
                   doSearch(inputSearch.value, example);
                }

                // Or update the query
                else {
                     doUpdateQuery(example.q, example);
                }
            }

            $('#' + config.examplesDiv + ' a').removeClass('active');
            $('#' + config.examplesDiv + ' a.' + example.id).toggleClass('active');

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

            yasr.selectPlugin('taxon');
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
        }
    }


    function doUpdateQuery(searchText, options)
    {


        searchText = searchText || inputSearch.value;
        if (!searchText) return; // Skip if empty

        let searchTerms = [searchText];
        if (searchText.indexOf('"') !== -1 || searchText.indexOf(',') !== -1) {
            searchTerms = searchText.split(/[",]+/).map(s => s.trim()).filter(s => s.length > 0);
            console.info("Multiple search:", searchTerms);
        }

        // Auto select example
        if (selectedExampleIndex === -1) {
            const autoSelectExampleIndex = examples.map((example, index) => {
                const count = searchTerms.reduce((count, searchTerm) =>
                        ((example.canHandleTerm && example.canHandleTerm(searchTerm)) ? count + 1 : count)
                    , 0);
                return {index,count};
            })
                .sort((e1, e2) => e1.count === e2.count ? 0 : (e1.count > e2.count ? -1 : 1))
                // Take the example that match must of search terms
                .map(e => e.index)[0];
            console.info("Auto select tab index:", autoSelectExampleIndex);
            showExample(autoSelectExampleIndex, {silent: true});
        }

        const example = examples[selectedExampleIndex];
        options = {
            ...example,
            limit: 50,
            ...options
        };
        options.q = undefined;

        try {
            initYase();

            let binding = {
                ...options.binding,
                limit: options.limit
            };

            let nbLoop = 0;
            let queryString = (options.query || defaultQuery);

            while (queryString.indexOf('{{') !== -1 && nbLoop < 10) {
                // Create where clause
                queryString = searchTerms.reduce((query, q) => {
                    const filterClause = (options.filters || [])
                        .map(key => filtersMap[key])
                        .join('\n\t&& ');

                    // Compute the query
                    query = query.replace('#filter', '\n\t|| (' + filterClause + ') #filter')
                        .replace('{{filter}}', '(' + filterClause + ') #filter');

                    // Bind params
                    binding.q = q;
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
            const prefixes = {};
            (options.prefixes || []).forEach(ns => {
                const example = examplePrefixes.find(p => p.ns === ns);
                prefixes[ns] = example && example.prefix || undefined;
            });
            yasqe.addPrefixes(prefixes);
        }
        catch(error) {
            console.error(error);
            onError({data: (error && error.message || error)});
        }
    }

    function showLoading(enable) {
        if (enable === false) {
            $('#loading').addClass('d-none');
        }
        else {
            $('#loading').removeClass('d-none');
        }
    }

    function receivedResponse(yasqe, req, duration) {
        log('RESPONSE: received in ' + duration + 'ms');

        initYasr();

        yasr.setResponse(req);

        showResult();
        showLoading(false);
    }

    function showResult(enable) {
        if (enable === false) {
            $('#' + config.yasr).addClass('d-none');
            $('#' + config.examplesDiv).addClass('d-none');
        }
        else {
            $('#' + config.yasr).removeClass('d-none');
            $('#' + config.examplesDiv).removeClass('d-none');
        }
    }

    window.addEventListener("load", init, false);

    return {
        showExample,
        showDebug,
        clearLog,
        displayExamples,
        doSearch
    }
}
