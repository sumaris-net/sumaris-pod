function AppTaxonSearch(config) {
    const defaultOptions = {
        yasgui: 'yasgui',
        yasqe: 'yasqe'
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


    const defaultQuery = "SELECT * WHERE {\n" +
        "  ?sub ?pred ?obj .\n" +
        "  {{where}} \n" +
        "} LIMIT {{limit}}";

    const filtersMap = {
        rdfType: '?sub rdf:type {{rdfType}} .',
        exact: 'filter( ?obj="{{q}}" )',
        prefixMatch: 'filter( regex( ?obj, "^{{q}}.*" ) )',
        anyMatch: 'filter( regexp( ?obj, ".*{{q}}.*" ) )',
    };
    const examples = {
        default: {
            name: 'Default',
            q: 'Lophius budegassa',
            prefixes: ['rdf', 'this'],
            query: defaultQuery,
            filters: ['rdfType'],
            binding: {
                rdfType: 'this:TaxonName'
            }
        }

    };

    // SparQL var
    let defaultEndpoint,
        defaultPrefixUri,
        endpoints;

    // Form elements
    let output,
        buttonSearch,
        inputSearch;

    // YasGui
    let yasgui, yasqe, yasr;

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

    function clearScreen()
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
        buttonSearch = document.getElementById("buttonSearch");
        inputSearch.value = examples.default.q;
        output = document.getElementById("output");

        // Collect endpoints, from default, endpoint map, and examples queries
        endpoints = Object.keys(endpointsById).map(key => endpointsById[key])
            .reduce((res, ep) => (!ep || res.findIndex(endpoint => endpoint === ep) !== -1) ? res : res.concat(ep),
                [defaultEndpoint]);
    }

    function showExample(name) {
        const example = examples[name];
        if (example) {
            inputSearch.value = example.q;
        }
    }

    function getEndpoints() {
        return endpoints;
    }

    function initYas() {

        const requestConfig = {
            endpoint: defaultEndpoint || endpoints.length && endpoints[0] || undefined
            // headers: () => ({
            //     //'token': '' // TODO add authenticated token
            // })
        };

        // if (!yasgui && options.yasgui) {
        //     const element = document.getElementById(options.yasgui);
        //     if (!element) throw new Error('Cannot find div with id=' + options.yasgui);
        //
        //     // Start YasGUI
        //     yasgui = new Yasgui(element, {
        //         requestConfig,
        //         copyEndpointOnNewTab: true,
        //         endpointCatalogueOptions: {
        //             getData: getEndpoints
        //         }
        //     });
        // }

        if (!yasqe) {
            const element = document.getElementById(config.yasqe);
            if (!element) throw new Error('Cannot find div with id=' + config.yasqe);

            yasqe = new Yasqe(element, {
                requestConfig
            });

            // Link editor to query
            yasqe.on("queryResponse", receivedResponse);

        }

        if (!yasr) {
            const element = document.getElementById(config.yasr);
            if (!element) throw new Error('Cannot find div with id=' + config.yasr);

            yasr = new Yasr(element, {
                requestConfig,
                pluginOrder: ["table", "response"]
            });


        }

    }

    function receivedResponse(yasqe, req, duration) {
        log('RESPONSE: received in ' + duration + 'ms');
        console.info(req);
        yasr.setResponse(req);
    }

    function showResult() {
        const element = document.getElementById(config.yasr);
        if (!element) throw new Error('Cannot find div with id=' + config.yasr);

        element.classList.remove('d-none');
    }

    /* -- Search -- */

    function doSearch(searchText, options)
    {
        options = {
            prefixes: ['this', 'rdf'],
            query: defaultQuery,
            filters: ['rdfType', 'prefixMatch'],
            bindings: {
                rdfType: 'this:TaxonName'
            },
            limit: 10,
            ...options
        };
        searchText = searchText || inputSearch.value;
        if (!searchText) return; // Skip if empty

        try {
            log("SEARCH: " + searchText, "text-muted");
            initYas();

            let bindings = {
                ...options.bindings,
                q: searchText,
                limit: options.limit
            };



            // Create where clausse
            const whereClause = (options.filters || [])
                .map(key => filtersMap[key])
                .reduce((res, filter) => {
                    return (res ? res + '\n\t' : '') +  filter;
                }, undefined);
            //log("FILTER: " + filterString);

            // Compute the query
            let queryString = (options.query || defaultQuery).replace('{{where}}', whereClause);
            queryString = Object.keys(bindings).reduce((query, key) => {
                return query.replace('{{' + key + '}}', bindings[key])
            }, queryString);
            yasqe.setValue(queryString);
            log("QUERY: " + queryString);

            // Add prefixes
            const prefixes = {};
            (options.prefixes || []).forEach(ns => {
                const example = examplePrefixes.find(p => p.ns === ns);
                prefixes[ns] = example && example.prefix || undefined;
            });
            yasqe.addPrefixes(prefixes);

            //showLoading();

            setTimeout(() => {
                yasqe.executeQuery();
            })

        }
        catch(error) {
            console.error(error);
            onError({data: (error && error.message || error)});
        }
    }




    window.addEventListener("load", init, false);

    return {
        showExample,
        clearScreen,
        doSearch
    }
}
