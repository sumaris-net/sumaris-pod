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
function AppRdfExporter() {

    let restBasePath = '/ontology',
        inputUri,
        inputFormat,
        inputWithSchema,
        withSchemaDiv,
        inputModelType,
        inputClassName,
        inputObjectId,
        inputDisjoints,
        inputPackages,
        uriDiv,
        paramsDiv,
        logDiv,
        outputTextarea;

    const classesByModelType = {
        'data': ['TaxonName', 'Location', 'Gear', 'Department'],
        'schema': ['Taxon', 'Location', 'Gear', 'Department', 'Trip']
    };

    function init() {
        inputUri = document.getElementById("baseUri");
        inputUri.oninput = computeBaseUri;

        inputModelType = document.getElementById("modelType");
        inputModelType.onchange = onModelTypeChanged;

        inputClassName = document.getElementById("className");
        inputObjectId = document.getElementById("objectId");

        inputFormat = document.getElementById("format");
        inputFormat.onchange = computeParams;

        inputWithSchema = document.getElementById("withSchema");
        inputWithSchema.onchange = computeParams;
        inputWithSchema.checked = true;
        withSchemaDiv = document.getElementById("withSchemaDiv");

        inputDisjoints = document.getElementById("disjoints");
        inputDisjoints.onchange = computeParams;

        inputPackages = document.getElementById("packages");
        if (inputPackages) inputPackages.oninput = computeParams;

        uriDiv = document.getElementById("uriDiv");
        paramsDiv = document.getElementById("paramsDiv");
        outputTextarea = document.getElementById("outputTextarea");
        logDiv = document.getElementById("logDiv");

        if (window.location && window.location.origin) {
            inputUri.value = window.location.origin + restBasePath + '/';
        }
        computeBaseUri();
        computeParams();
        onModelTypeChanged();
    }

    function logInfo(message, cssClass) {
        logToHtml(message, cssClass || 'text-info');
        console.info(message);
    }

    function logWarning(message) {
        logToHtml('WARNING: ', 'text-warning');
        console.warn(message);
    }

    function logError(message, error) {
        logToHtml('ERROR: ' + message, 'text-error');
        if (error) {
            console.error(message, error);
        } else {
            console.error(message);
        }
    }

    function computeBaseUri() {
        let baseUri = inputUri.value;
        if (baseUri.lastIndexOf('/') !== baseUri.length - 1) {
            baseUri += '/';
        }

        console.info("New base URI: " + baseUri);

        uriDiv.innerHTML = baseUri;

        return baseUri;
    }

    function computeParams() {

        const params = [];
        let needFormat = false;
        const modelType = inputModelType.value;

        if (inputFormat.value !== "rdf") {
            params.push('format=' + inputFormat.value) // True by default
            needFormat = true;
        }
        if (modelType === 'data' && inputWithSchema.checked) {
            params.push('schema=true') // True by default
        }
        if (!inputDisjoints.checked) {
            params.push('disjoints=false') // True by default
        }
        if (inputPackages && inputPackages.value) {
            params.push('packages=' + inputPackages.value)
        }

        const notFormatParams = params.length && params.slice(needFormat ? 1 : 0) || [];
        paramsDiv.innerHTML = notFormatParams.length ? ('&' + notFormatParams.join('&')) : '';

        return (params.length) ? ('?' + params.join('&')) : '';
    }

    function onModelTypeChanged() {

        const modelType = inputModelType.value;
        console.info("Model type changed to: " + modelType);

        // Add classes
        fillClassNames(classesByModelType[modelType]);

        if (modelType === "schema") {
            inputObjectId.classList.add('d-none');
            withSchemaDiv.classList.add('d-none');// Disable schema export
            inputWithSchema.checked = false;
        }
        // Model type == data
        else if (modelType === "data") {
            inputWithSchema.checked = false; // Disable by default
            withSchemaDiv.classList.remove('d-none');

            if (!inputClassName.value || !inputClassName.value.trim().length) {
                inputObjectId.classList.add('d-none');
            }
            else {
                inputObjectId.classList.remove('d-none');
            }
        }

        computeParams();
    }

    function createRequestUri() {
        let path = computeBaseUri();
        const modelType = inputModelType.value;

        // Add domain
        path += modelType + '/';

        // Add class name (if exists)
        if (inputClassName.value && inputClassName.value.trim().length) {
            path += inputClassName.value + '/';
        }

        // Add object id(if exists)
        if (modelType === 'data' && inputObjectId.value && inputObjectId.value.trim().length) {
            path += inputObjectId.value;
        }

        // Add params
        path += computeParams();

        return path;
    }

    function fillClassNames(classes) {
        inputClassName.innerHTML = "";
        if (classes && classes.length) {
            inputClassName.classList.remove('d-none');
            for (let i = 0; i < classes.length; i++) {
                var opt = document.createElement('option');
                opt.value = classes[i];
                opt.innerHTML = classes[i];
                inputClassName.appendChild(opt);
            }
        }
        else {
            inputClassName.classList.add('d-none');
        }
    }

    function executeRequest(requestUri) {

        requestUri = requestUri || createRequestUri();

        try {

            logInfo("GET: " + requestUri, "text-muted");
            const xmlhttp = new XMLHttpRequest();
            const now = Date.now();
            outputTextarea.value = 'Loading...';
            xmlhttp.onreadystatechange = function () {
                if (xmlhttp.readyState == XMLHttpRequest.LOADING) {   // XMLHttpRequest.LOADING == 3
                    outputTextarea.value = xmlhttp.responseText;
                } else if (xmlhttp.readyState == XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
                    if (xmlhttp.status == 200) {
                        var execTimeMs = Date.now() - now;
                        outputTextarea.value = xmlhttp.responseText;
                        logInfo('Response received in ' + execTimeMs + 'ms');
                    } else if (xmlhttp.status == 400 || xmlhttp.status == 404) {
                        logError('Http error ' + xmlhttp.status + ' (not found)');
                    } else {
                        logError('Unknown error (code=' + xmlhttp.status + ')', xmlhttp.error);
                    }
                }
            };

            xmlhttp.open("GET", requestUri, true);
            xmlhttp.send();

        } catch (error) {
            console.error(error);
            logError({data: (error && error.message || error)});
        }
    }

    function openViewVowl(requestUri){
        requestUri = requestUri || createRequestUri();

        // Remove the format parameters
        requestUri = requestUri.replace(/format=[a-z-A-Z-_0-9]+[&]?/, "");
        if (requestUri.endsWith('?')) {
            requestUri = requestUri.substr(0, requestUri.length-1);
        }

        let webvowlUrl = inputUri.value;
        if (!webvowlUrl.endsWith('/')) {
            webvowlUrl += '/'
        }
        webvowlUrl += 'webvowl#iri=' + encodeURI(requestUri);

        window.open(webvowlUrl, '_system', null, true);
    }

    function logToHtml(message, classAttribute) {
        const pre = document.createElement("p");
        if (classAttribute) {
            const classes = classAttribute.split(" ");
            for (let i = 0; i < classes.length; i++) {
                pre.classList.add(classes[i]);
            }
        }
        pre.style.wordWrap = "break-word";
        pre.innerHTML = message;
        logDiv.appendChild(pre);
    }

    function clearScreen() {
        logDiv.innerHTML = "";
        outputTextarea.value = "";
    }

    window.addEventListener("load", init, false);

    const exports = {
        clearScreen,
        executeRequest,
        openViewVowl
    };

    return exports;
}
