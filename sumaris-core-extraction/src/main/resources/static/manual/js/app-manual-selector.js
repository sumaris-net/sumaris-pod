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
function AppManualSelector() {

    let restBasePath = '/api/extraction',
        inputUri,
        uriDiv,
        inputCategory,
        inputExtractionType,
        inputFormat,
        paramsDiv,
        logDiv,
        outputTextarea,
        types;


    function init() {
        inputUri = document.getElementById("baseUri");
        inputUri.oninput = computeBaseUri;

        uriDiv = document.getElementById("uriDiv");

        inputCategory = document.getElementById("category");
        inputCategory.onchange = onCategoryChanged;

        inputExtractionType = document.getElementById("extractionType");
        inputFormat = document.getElementById("format");
        paramsDiv  = document.getElementById("paramsDiv");
        outputTextarea = document.getElementById("outputTextarea");
        logDiv = document.getElementById("logDiv");

        if (window.location && window.location.origin) {
            inputUri.value = window.location.origin + restBasePath + '/';
        }
        computeBaseUri();

        onCategoryChanged();
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

        if (inputFormat.value !== "md") {
            params.push('format=' + inputFormat.value) // True by default
            needFormat = true;
        }

        const notFormatParams = params.length && params.slice(needFormat ? 1 : 0) || [];
        paramsDiv.innerHTML = notFormatParams.length ? ('&' + notFormatParams.join('&')) : '';

        return (params.length) ? ('?' + params.join('&')) : '';
    }

    function onCategoryChanged() {

        const category = inputCategory.value;
        console.info("Category changed to: " + category);

        // Add types
        fillExtractionTypes(category);

        computeParams();
    }

    function fillExtractionTypes(category) {
        let path = computeBaseUri();
        path += 'types';

        category = category && category.toUpperCase();

        const onReceivedResponse = function(response) {
            types = response && JSON.parse(response) || [];
            console.info("Loaded types: ", types);

            inputExtractionType.innerHTML = types.reduce((res, type) => {
                // Filter on same category
                if (category && type.category !== category) return res;
                return res + '<option value=' + type.label + '>' + (type.name || type.label) + '</option>\n';
            }, "\n");
        }

        executeRequest(path, onReceivedResponse, 'application/json');
    }

    function createRequestUri() {
        let path = computeBaseUri();
        path += 'manual/';

        // Add category
        const category = inputCategory.value;
        path += category + '/';

        // Add extraction type
        const extractionType = inputExtractionType.value;
        path += extractionType;

        // Add params
        path += computeParams();

        return path;
    }

    function executeRequest(requestUri, callback, acceptHeader) {

        requestUri = requestUri || createRequestUri();

        try {

            logInfo("GET: " + requestUri, "text-muted");
            const xmlhttp = new XMLHttpRequest();
            const now = Date.now();
            xmlhttp.onreadystatechange = function () {
                if (xmlhttp.readyState === XMLHttpRequest.OPENED) {
                    xmlhttp.setRequestHeader('Accept', acceptHeader || 'application/html, text/markdown, text/plain')
                } else if (xmlhttp.readyState === XMLHttpRequest.LOADING) {   // XMLHttpRequest.LOADING == 3
                    outputTextarea.value = 'Loading...';
                } else if (xmlhttp.readyState === XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
                    if (xmlhttp.status === 200) {
                        var execTimeMs = Date.now() - now;

                        // Send response to callback, if any
                        if (callback && typeof callback == "function") {
                            callback(xmlhttp.responseText);
                        }

                        // Other wise, add response to output div
                        else {
                            outputTextarea.value = xmlhttp.responseText;
                            outputTextarea.classList.remove('d-none');
                            logInfo('Response received in ' + execTimeMs + 'ms');
                        }
                    } else if (xmlhttp.status === 400 || xmlhttp.status === 404) {
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

    function openManual() {
        const path = createRequestUri();

        window.open(path, '_system', null, true);
    }

    window.addEventListener("load", init, false);

    const exports = {
        clearScreen,
        executeRequest,
        openManual
    };

    return exports;
}
