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
function AppManualPreview() {

    let restBasePath = '/api/extraction',
        baseUri,
        inputCategory,
        inputExtractionType,
        previewDiv,
        sourceDiv,
        types;

    function init() {

        inputExtractionType = document.getElementById("extractionType");
        inputExtractionType.onchange = onExtractionTypeChanged;

        previewDiv = document.getElementById('preview');
        sourceDiv = document.getElementById('markdown');

        computePreview();

        computeBaseUri();

        fillExtractionTypes();
    }

    function logInfo(message, cssClass) {
        console.info(message);
    }

    function logWarning(message) {
        console.warn(message);
    }

    function logError(message, error) {
        if (error) {
            console.error(message, error);
        } else {
            console.error(message);
        }
    }

    function computePreview(markdown) {
        if (markdown) {
            sourceDiv.innerHTML = markdown;
        }
        else {
            markdown = sourceDiv.innerHTML;
        }

        // Compute markdown preview
        previewDiv.innerHTML = marked(markdown);
    }

    function computeBaseUri() {
        if (window.location && window.location.origin) {
            baseUri = window.location.origin + restBasePath + '/';
        }
        if (baseUri.lastIndexOf('/') !== baseUri.length - 1) {
            baseUri += '/';
        }

        console.info("New base URI: " + baseUri);

        return baseUri;
    }

    function onExtractionTypeChanged() {

        const type = inputExtractionType.value;
        console.info("Extraction type changed to: " + type);

        let path = computeBaseUri();
        path += type + '.md';

        // Run the request
        executeRequest(path, computePreview, 'text/markdown');
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
                const value = type.category + '/' + type.label
                const name = type.category + ' > ' + (type.name || type.label);
                return res + '<option value=' + value + '>' + name + '</option>\n';
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
                } else if (xmlhttp.readyState === XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
                    if (xmlhttp.status === 200) {
                        // Send response to callback
                        callback(xmlhttp.responseText);
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

    window.addEventListener("load", init, false);

    return {}; // Exports
}
