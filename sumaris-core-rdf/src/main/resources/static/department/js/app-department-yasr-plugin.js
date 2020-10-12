/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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



function YasrDepartmentPlugin(yasr) {
    // A priority value. If multiple plugin support rendering of a result, this value is used
    // to select the correct plugin
    this.priority = 10;

    // Name
    this.label = "Department";

    // Whether to show a select-button for this plugin
    this.hideFromSelection = false;

    // Max length, before truncated URI
    this.uriMaxLength = undefined;

    // Default options
    //defaults = YasrDepartmentPlugin.defaults;

    this.yasr = yasr;

    /**
     * Group bindings, using URI as unique key
     * @param bindings
     * @returns {*[]}
     */
    this.getItemsFromBindings = function(bindings) {
        const itemByUri = {};
        bindings.forEach(binding => {
            const missing = binding.lookup && binding.lookup.found === false;
            const uniqueKey = (binding.sourceUri && binding.sourceUri.value) || (missing && binding.lookup.value);
            if (uniqueKey) {
                if (!itemByUri[uniqueKey]) {
                    itemByUri[uniqueKey] = {
                        uri: binding.sourceUri && binding.sourceUri.value,
                        label: binding.label && binding.label.value,
                        address: binding.address && binding.address.value,
                        created: binding.created && binding.created.value,
                        modified: binding.modified && binding.modified.value,
                        exactMatch : [],
                        seeAlso : [],
                        parentUri: binding.parent && binding.parent.value,
                        missing
                    };
                    // Remove modified date, if same as created
                    if (itemByUri[uniqueKey].modified && itemByUri[uniqueKey].modified === itemByUri[uniqueKey].created) {
                        itemByUri[uniqueKey].modified = undefined;
                    }
                }
                if (binding.exactMatch && binding.exactMatch.value
                    // Exclude if already present
                    && !itemByUri[uniqueKey].exactMatch.includes(binding.exactMatch.value)
                    // Exclude is same as source
                    && binding.exactMatch.value.trim() !== uniqueKey) {
                    itemByUri[uniqueKey].exactMatch.push(binding.exactMatch.value.trim())
                }
                if (binding.seeAlso && binding.seeAlso.value
                    // Exclude if already present
                    && !itemByUri[uniqueKey].seeAlso.includes(binding.seeAlso.value)
                    // Exclude is same as source
                    && binding.seeAlso.value.trim() !== uniqueKey) {
                    itemByUri[uniqueKey].seeAlso.push(binding.seeAlso.value.trim())
                }
            }
        });
        return Object.keys(itemByUri).map(key => itemByUri[key]).sort((t1, t2) => t1.label === t2.label ? 0 : (t1.label > t2.label ? 1 : -1));
    }

    // Draw the resultset. This plugin simply draws the string 'True' or 'False'
    this.draw = function () {
        const el = document.createElement("div");
        el.classList.add('department-plugin');

        const hasResults = this.yasr.results && this.yasr.results.json && true;

        const prefixes  = this.yasr.getPrefixes();

        // Get items
        const items = hasResults && this.getItemsFromBindings(this.yasr.results.json.results.bindings);

        const labelFirst = hasResults && this.yasr.results.json.head.vars.findIndex(v => v === "label") === 0;

        const headerCols = ["  <th scope='col'>#</th>",
            "  <th scope='col'>Name</th>",
            "  <th scope='col'>Id / Date</th>",
            "  <th scope='col'>Address</th>",
            "  <th scope='col'>Parent</th>",
            "  <th scope='col'>Exact match / see also</th>"
        ];

        // Inverse
        if (!labelFirst) this.inverseArrayValue(headerCols, 1, 2);

        let rows = (hasResults && items || []).map((item, index) => {
            let rowCols = [

                // Index
                "<th scope=\"row\">" + (index + 1) + "</th>",

                // Name
                "<td class='col'>" + (item.label || '') + "</td>",

                // Source URI
                "<td>" +
                ((item.missing) ?
                    '<span style="color: red;"><i class="icon ion-close"></i>' + (item.uri|| '') + ' <i>(missing)</i></span>' :
                    this.displayUri(item.uri, prefixes, this.uriMaxLength)) +
                (item.created ? ("<br/><small class='gray' title='Creation date'><i class='icon ion-calendar'></i> "+ item.created + "</small>") : "") +
                (item.modified ? ("<br/><small class='gray' title='Last modification date'><i class='icon ion-pencil'></i> "+ item.modified + "</small>") : "") +
                "</td>",

                // Address
                "<td class='col'>" +
                (item.address ? ("<small> "+ item.address.replace(',', '<br/>') + "</small>") : "") +
                "</td>",

                // Parent
                "<td>" +
                this.displayUri(item.parentUri, prefixes, this.uriMaxLength) +
                "</td>",

                "<td class='exact-match'>" +
                // Exact match
                (item.exactMatch || []).map(uri => this.displayUri(uri, prefixes, this.uriMaxLength)).join("<br/>\n") +

                // seeAlso (with a separator)
                (item.exactMatch.length && item.seeAlso.length ?
                    ("<br/><small><i>See also:</i></small>\n<ul>" +
                    (item.seeAlso || []).map(uri => "<li>" + this.displayUri(uri, prefixes, this.uriMaxLength)).join("\n") +
                    "</ul>")
                : '') +
                "</td>"

            ];
            if (!labelFirst) this.inverseArrayValue(rowCols, 1, 2);

            return "<tr>" + rowCols.join('\n') + "</tr>";
        });

        if (!rows.length) {
            rows = [" <tr><td colspan='" + headerCols.length + "'>" +
                "No data available" +
                " </td></tr>"];
        }

        el.innerHTML = "<table class='table table-striped'>" +
            "<thead>" +
            " <tr>" +
                headerCols.join('\n') +
            " </tr>" +
            "</thead>" +
            rows.join('\n') +
            "</table>";



        this.yasr.resultsEl.appendChild(el);
    }

    // A required function, used to indicate whether this plugin can draw the current
    // resultset from yasr
    this.canHandleResults = function() {
        return (
            this.yasr.results.type === 'json' && this.yasr.results.json.head
            && this.yasr.results.json.head.vars.includes('label')
            && this.yasr.results.json.head.vars.includes('sourceUri')
        );
    }
    // A required function, used to identify the plugin, works best with an svg
    this.getIcon = function() {
        const textIcon = document.createElement("div");
        textIcon.classList.add("plugin_icon", "txtIcon");
        textIcon.innerText = "✓";
        return textIcon;
    }

    this.download = function() {
        const hasResults = this.yasr.results && this.yasr.results.json && this.yasr.results.json.results.bindings.length && true;
        if (!hasResults) return undefined;

        return {
            getData: () => this.yasr.results.asCsv(),
            contentType:"text/csv",
            title:"Download result",
            filename:"organization.csv"
        };
    }

    /* -- Internal functions -- */

    this.inverseArrayValue = function(array, index1, index2) {
        let temp1 = array[index1];
        array[index1] = array[index2];
        array[index2] = temp1;
    }

    this.urnToUrl = function(uri) {
        if (!uri || !uri.startsWith('urn:')) return uri;

        // TODO add urn:  mapping to URL

        return uri;
    }

    this.urnToRdfUrl = function(uri) {
        if (!uri || !uri.startsWith('urn:')) return uri;

        // Resolve Life science ID (e.g. WoRMS urn, etc.)
        if (uri.startsWith("urn:lsid:")) {
            return "http://www.lsid.info/resolver/api.php?lsid=" + uri;
        }

        return uri;
    }

    this.simplifyUri = function(uri, prefixes, truncLength) {
        if (uri && prefixes) {
            for (let prefix of Object.keys(prefixes)) {
                const namespace = prefixes[prefix];
                const index = namespace ? uri.trim().indexOf(namespace) : -1;
                if (index === 0) {
                    return prefix + ':' + uri.trim().substr(namespace.length);
                }
            }
        }

        // No prefix found
        return this.truncText(uri, truncLength);
    }


    this.truncText = function(text, truncLength) {
        // No prefix found: check length
        if (truncLength && truncLength > 3 && text.length > truncLength) {
            return text.substr(0, truncLength-3) + '...';
        }
        return text;
    }

    this.displayUri = function(uri, prefixes, textTruncLength) {
        if (!uri) return '';

        let getStartTag;
        if (this.defaults && this.defaults.onUriClickTarget) {
            const target = this.defaults.onUriClickTarget;
            getStartTag = function(url) {
                return "<a href='" + url + "' target='"+ target +"' >";
            }
        }
        else if (this.defaults && this.defaults.onUriClick) {
            const onUriClick = this.defaults.onUriClick;
            getStartTag = function(url) {
                return "<a href='#' onclick='"+ onUriClick.replace('{{url}}', "\"" + url + "\"") +"' >";
            }
        }
        else {
            getStartTag = function(url) {
                return "<a href='" + url + "'>";
            }
        }
        if (uri.startsWith('http')) {
            return getStartTag(uri) + this.simplifyUri(uri, prefixes, textTruncLength) + "</a>";
        }
        if (uri.startsWith('urn:')) {
            const url = this.urnToUrl(uri);
            const rdfUrl = this.urnToRdfUrl(uri);
            let html = getStartTag(url) + this.truncText(uri, textTruncLength) + "</a>";

            if (rdfUrl && url !== rdfUrl) {
                html += "&nbsp;" + getStartTag(rdfUrl) + "(rdf)</a>";
            }
            return html;
        }
        return uri;
    }
}

YasrDepartmentPlugin.prototype.defaults = {
    onUriClickTarget : undefined,
    onUriClick : undefined
};