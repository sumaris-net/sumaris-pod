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
function AppDocPreview() {

    let restBasePath = '/api/extraction',
        baseUri,
        inputExtractionType,
        previewDiv,
        sourceDiv,
        types;

    function init() {

        inputExtractionType = document.getElementById("extractionType");
        inputExtractionType.onchange = onExtractionTypeChanged;

        previewDiv = document.getElementById('preview');
        sourceDiv = document.getElementById('markdown');

        updatePreview();

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

    function updatePreview(markdown) {
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

        const extractionTypePath = inputExtractionType.value;
        console.info("Extraction type changed to: " + extractionTypePath);

        let path = computeBaseUri();
        path += 'doc/';

        path += extractionTypePath.toLowerCase() + '.md';

        // Run the request
        executeRequest(path, updatePreview, 'text/markdown');
    }

    function fillExtractionTypes(filterCategory) {
        let path = computeBaseUri();
        path += 'types';

        filterCategory = filterCategory && filterCategory.toLowerCase();

        const onReceivedResponse = function(response) {
            types = response && JSON.parse(response) || [];
            console.info("Loaded types: ", types);

            inputExtractionType.innerHTML = types.reduce((res, type) => {
                const category = type.category.toLowerCase();

                // Filter on same category
                if (filterCategory && category !== filterCategory) return res; // Skip

                const value = category + '/' + type.label;
                const name = category + ' > ' + (type.name || type.label);
                return res + '<option value=' + value + '>' + name + '</option>\n';
            }, "\n");
        }

        executeRequest(path, onReceivedResponse, 'application/json');
    }

    function executeRequest(requestUri, callback, acceptHeader) {
        if (!requestUri) throw Error('Missing requestUri');

        try {

            logInfo("GET: " + requestUri, "text-muted");
            const request = new XMLHttpRequest();
            const now = Date.now();
            request.onreadystatechange = function () {
                if (request.readyState === XMLHttpRequest.OPENED) {
                    request.setRequestHeader('Accept', acceptHeader || 'application/html, text/markdown, text/plain')
                } else if (request.readyState === XMLHttpRequest.DONE) {   // XMLHttpRequest.DONE == 4
                    if (request.status === 200) {
                        // Send response to callback
                        callback(request.responseText);
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

    window.addEventListener("load", init, false);

    return {}; // Exports
}
