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

function inverseArrayValue(array, index1, index2) {
    let temp1 = array[index1];
    array[index1] = array[index2];
    array[index2] = temp1;
}
function urnToUrl(uri) {
    if (!uri || !uri.startsWith('urn:')) return uri;

    // WoRMS
    if (uri.startsWith("urn:lsid:marinespecies.org:taxname:")) {
        const parts = uri.split(':');
        return "http://www.marinespecies.org/aphia.php?p=taxdetails&id=" + parts[parts.length - 1];
    }

    // Resolve Life science ID
    if (uri.startsWith("urn:lsid:")) {
        return "http://www.lsid.info/resolver/?lsid=" + uri;
    }

    return uri;
}

function urnToRdfUrl(uri) {
    if (!uri || !uri.startsWith('urn:')) return uri;

    // Resolve Life science ID (e.g. WoRMS urn, etc.)
    if (uri.startsWith("urn:lsid:")) {
        return "http://www.lsid.info/resolver/api.php?lsid=" + uri;
    }

    return uri;
}



function simplifyUri(uri, prefixes, truncLength) {
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
    return truncText(uri, truncLength);
}


function truncText(text, truncLength) {
    // No prefix found: check length
    if (truncLength && truncLength > 3 && text.length > truncLength) {
        return text.substr(0, truncLength-3) + '...';
    }
    return text;
}

function displayUri(uri, prefixes, textTruncLength) {
    if (!uri) return '';

    if (uri.startsWith('http')) {
        return "<a href='" + uri + "'>" + simplifyUri(uri, prefixes, textTruncLength) + "</a>";
    }
    if (uri.startsWith('urn:')) {
        const url = urnToUrl(uri);
        const rdfUrl = urnToRdfUrl(uri);
        let html = "<a href='" + url + "'>" + truncText(uri, textTruncLength) + "</a>";

        if (rdfUrl && url !== rdfUrl) {
            html += "&nbsp;<a href='" + rdfUrl + "'>(rdf)</a>";
        }
        return html;
    }
    return uri;
}

class YasrTaxonPlugin {
    // A priority value. If multiple plugin support rendering of a result, this value is used
    // to select the correct plugin
    priority = 10;

    // Name
    label = "Taxon";

    // Whether to show a select-button for this plugin
    hideFromSelection = false;

    // Max length, before truncated URI
    uriMaxLength = undefined;

    constructor(yasr) {
        this.yasr = yasr;
    }

    getTaxonsFromBindings(bindings) {
        const taxonsByUri = {};
        bindings.forEach(binding => {
            const uri = binding.sourceUri.value;
            if (!taxonsByUri[uri]) {
                taxonsByUri[uri] = {
                    uri,
                    scientificName: binding.scientificName.value,
                    author: binding.author && binding.author.value,
                    rank: binding.rank && binding.rank.value,
                    created: binding.created && binding.created.value,
                    modified: binding.modified && binding.modified.value,
                    seeAlso : [],
                    exactMatch : [],
                    parentUri: binding.parent && binding.parent.value
                };
                // Remove modified date, if same as created
                if (taxonsByUri[uri].modified && taxonsByUri[uri].modified === taxonsByUri[uri].created) {
                    taxonsByUri[uri].modified = undefined;
                }
            }
            if (binding.exactMatch && binding.exactMatch.value
                // Exclude if already present
                && !taxonsByUri[uri].exactMatch.includes(binding.exactMatch.value)
                // Exclude is same as source
                && binding.exactMatch.value.trim() !== uri) {
                taxonsByUri[uri].exactMatch.push(binding.exactMatch.value.trim())
            }
            if (binding.seeAlso && binding.seeAlso.value
                // Exclude if already present
                && !taxonsByUri[uri].seeAlso.includes(binding.seeAlso.value)
                // Exclude is same as source
                && binding.seeAlso.value.trim() !== uri) {
                taxonsByUri[uri].seeAlso.push(binding.seeAlso.value.trim())
            }
        });
        return Object.keys(taxonsByUri).map(key => taxonsByUri[key]).sort((t1, t2) => t1.scientificName === t2.scientificName ? 0 : (t1.scientificName > t2.scientificName ? 1 : -1));
    }

    // Draw the resultset. This plugin simply draws the string 'True' or 'False'
    draw() {
        const el = document.createElement("div");
        el.classList.add('taxon-plugin');

        const prefixes  = this.yasr.getPrefixes();

        // Get taxon list
        const taxons = this.getTaxonsFromBindings(this.yasr.results.json.results.bindings);

        const scientificNameFirst = this.yasr.results.json.head.vars.findIndex(v => v === "scientificName") === 0;
        const hasAuthor = this.yasr.results.json.head.vars.findIndex(v => v === "author") !== -1;
        const hasRank = this.yasr.results.json.head.vars.findIndex(v => v === "rank") !== -1;

        const headerCols = ["  <th scope='col'>#</th>",
            "  <th scope='col'>Scientific name</th>",
            "  <th scope='col'>Id / Date</th>",
            "  <th scope='col'>Author</th>",
            "  <th scope='col'>Rank</th>",
            "  <th scope='col'>Parent</th>",
            "  <th scope='col'>Exact match / seeAlso</th>"
        ];

        // Inverse
        if (!scientificNameFirst) inverseArrayValue(headerCols, 1, 2);

        // Mask unused columns
        if (!hasAuthor) headerCols[3] = "";
        if (!hasRank) headerCols[4] = "";

        const rows = taxons.map((taxon, index) => {
            let rowCols = [

                // Index
                "<th scope=\"row\">" + (index + 1) + "</th>",

                // Scientific name
                "<td class='col'>" + taxon.scientificName + "</td>",

                // Source URI
                "<td>" + displayUri(taxon.uri, prefixes, this.uriMaxLength) +
                (taxon.created ? ("<br/><small class='gray' title='Creation date'><i class='icon ion-calendar'></i> "+ taxon.created + "</small>") : "") +
                (taxon.modified ? ("<br/><small class='gray' title='Last modification date'><i class='icon ion-pencil'></i> "+ taxon.modified + "</small>") : "") +
                "</td>",

                // Author
                "<td class='col'>" + (taxon.author || '') + "</td>",

                // Rank
                "<td class='col'>" +
                displayUri(taxon.rank, prefixes, this.uriMaxLength) +
                "</td>",

                // Parent
                "<td>" +
                displayUri(taxon.parentUri, prefixes, this.uriMaxLength) +
                "</td>",

                "<td class='exact-match'>" +
                // Exact match
                (taxon.exactMatch || []).map(uri => displayUri(uri, prefixes, this.uriMaxLength)).join("<br/>\n") +

                // seeAlso (with a separator)
                (taxon.exactMatch.length && taxon.seeAlso.length ?
                    ("<br/><small><i>See also:</i></small>\n<ul>" +
                    (taxon.seeAlso || []).map(uri => "<li>" + displayUri(uri, prefixes, this.uriMaxLength)).join("\n") +
                    "</ul>")
                : '') +
                "</td>"

            ];
            if (!scientificNameFirst) inverseArrayValue(rowCols, 1, 2);
            if (!hasAuthor) rowCols[3] = "";
            if (!hasRank) rowCols[4] = "";

            return "<tr>" + rowCols.join('\n') + "</tr>";
        });

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
    canHandleResults() {
        return (
            this.yasr.results.type === 'json' && this.yasr.results.json.head
            && this.yasr.results.json.head.vars.includes('scientificName')
            && this.yasr.results.json.head.vars.includes('sourceUri')
        );
    }
    // A required function, used to identify the plugin, works best with an svg
    getIcon() {
        const textIcon = document.createElement("div");
        textIcon.classList.add("plugin_icon", "txtIcon");
        textIcon.innerText = "✓";
        return textIcon;
    }


}