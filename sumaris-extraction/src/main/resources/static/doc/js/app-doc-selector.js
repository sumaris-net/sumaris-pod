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
function AppDocSelector() {

    let restBasePath = '/api/extraction',
        inputUri,
        baseUri,
        uriDiv,
        inputCategory,
        inputExtractionType,
        inputFormat,
        paramsDiv,
        logDiv,
        outputDiv,
        previewDiv,
        sourceDiv,
        markdownDiv,
        types;

    function init() {
        inputUri = document.getElementById("baseUri");
        inputUri.oninput = computeBaseUri;

        uriDiv = document.getElementById("uriDiv");

        inputExtractionType = document.getElementById("extractionType");
        inputFormat = document.getElementById("format");
        paramsDiv  = document.getElementById("paramsDiv");
        outputDiv = document.getElementById("output");
        previewDiv = document.getElementById("preview");
        sourceDiv = document.getElementById("source");
        markdownDiv = document.getElementById("markdown");
        logDiv = document.getElementById("logDiv");

        computeBaseUri();

        fillExtractionTypes();
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

        baseUri = inputUri.value;
        if (!baseUri && window.location && window.location.origin) {
            baseUri = window.location.origin + restBasePath + '/';
            inputUri.value = baseUri;
        }
        if (baseUri && baseUri.lastIndexOf('/') !== baseUri.length - 1) {
            baseUri += '/';
        }

        console.info("New base URI: " + baseUri);

        uriDiv.innerHTML = baseUri  + 'doc/';

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

    function fillExtractionTypes() {
        let path = computeBaseUri();
        path += 'types';

        const onReceivedResponse = function(response) {
            types = response && JSON.parse(response) || [];
            console.info("Loaded types: ", types);

            inputExtractionType.innerHTML = types.reduce((res, type) => {
                return res + '<option value=' + type.label + '>' + (type.name || type.label) + '</option>\n';
            }, "\n");
        }

        executeRequest(path, onReceivedResponse, 'application/json');
    }

    function createRequestUri() {
        let path = computeBaseUri();
        path += 'doc/';

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
            const request = new XMLHttpRequest();
            const now = Date.now();
            request.onreadystatechange = function () {
                if (request.readyState === XMLHttpRequest.OPENED) {
                    request.setRequestHeader('Accept', acceptHeader || 'application/html, text/markdown, text/plain')
                } else if (request.readyState === XMLHttpRequest.LOADING) {   // XMLHttpRequest.LOADING == 3
                    previewDiv.innerHTML = '<br/><i>Loading...</i>';
                    sourceDiv.innerHTML = '';
                    $('#html-source').addClass('d-none');

                } else if (request.readyState === XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
                    if (request.status === 200) {
                        var execTimeMs = Date.now() - now;
                        logInfo('Response received in ' + execTimeMs + 'ms');

                        // Send response to callback, if any
                        if (callback && typeof callback == "function") {
                            callback(request.responseText);
                        }

                        // Other wise, add response to output div
                        else {

                            if (inputFormat.value === "md") {
                                previewDiv.innerHTML = marked(request.responseText);
                                sourceDiv.innerHTML = '<pre>' + request.responseText + '</pre>';

                                $('#html-source').html(''); // Remove html source content
                                $('#output .nav-tabs').removeClass('d-none'); // Show tabs header
                            }
                            else if (inputFormat.value === "html") {
                                previewDiv.innerHTML = '';

                                $('#html-source').text(request.responseText);
                                $('#html-source').removeClass('d-none');

                                $('#output .nav-tabs').addClass('d-none'); // Hide tabs header
                            }

                            outputDiv.classList.remove('d-none'); // Show output

                        }
                    } else if (request.status === 400 || request.status === 404) {
                        logError('Http error ' + request.status + ' (not found)');
                    } else {
                        logError('Unknown error (code=' + request.status + ')', request.error);
                    }
                }
            };

            request.open("GET", requestUri, true);
            request.send();

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
