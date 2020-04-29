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

    // Default options
    //defaults = YasrTaxonPlugin.defaults;

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

        const hasResults = this.yasr.results && this.yasr.results.json && true;

        const prefixes  = this.yasr.getPrefixes();

        // Get taxon list
        const taxons = hasResults && this.getTaxonsFromBindings(this.yasr.results.json.results.bindings);


        const scientificNameFirst = hasResults && this.yasr.results.json.head.vars.findIndex(v => v === "scientificName") === 0;
        const hasAuthor = hasResults && this.yasr.results.json.head.vars.findIndex(v => v === "author") !== -1;
        const hasRank = hasResults && this.yasr.results.json.head.vars.findIndex(v => v === "rank") !== -1;

        const headerCols = ["  <th scope='col'>#</th>",
            "  <th scope='col'>Scientific name</th>",
            "  <th scope='col'>Id / Date</th>",
            "  <th scope='col'>Author</th>",
            "  <th scope='col'>Rank</th>",
            "  <th scope='col'>Parent</th>",
            "  <th scope='col'>Exact match / see also</th>"
        ];

        // Inverse
        if (!scientificNameFirst) this.inverseArrayValue(headerCols, 1, 2);

        // Mask unused columns
        if (!hasAuthor) headerCols[3] = "";
        if (!hasRank) headerCols[4] = "";

        let rows = (hasResults && taxons || []).map((taxon, index) => {
            let rowCols = [

                // Index
                "<th scope=\"row\">" + (index + 1) + "</th>",

                // Scientific name
                "<td class='col'>" + taxon.scientificName + "</td>",

                // Source URI
                "<td>" + this.displayUri(taxon.uri, prefixes, this.uriMaxLength) +
                (taxon.created ? ("<br/><small class='gray' title='Creation date'><i class='icon ion-calendar'></i> "+ taxon.created + "</small>") : "") +
                (taxon.modified ? ("<br/><small class='gray' title='Last modification date'><i class='icon ion-pencil'></i> "+ taxon.modified + "</small>") : "") +
                "</td>",

                // Author
                "<td class='col'>" + (taxon.author || '') + "</td>",

                // Rank
                "<td class='col'>" +
                this.displayUri(taxon.rank, prefixes, this.uriMaxLength) +
                "</td>",

                // Parent
                "<td>" +
                this.displayUri(taxon.parentUri, prefixes, this.uriMaxLength) +
                "</td>",

                "<td class='exact-match'>" +
                // Exact match
                (taxon.exactMatch || []).map(uri => this.displayUri(uri, prefixes, this.uriMaxLength)).join("<br/>\n") +

                // seeAlso (with a separator)
                (taxon.exactMatch.length && taxon.seeAlso.length ?
                    ("<br/><small><i>See also:</i></small>\n<ul>" +
                    (taxon.seeAlso || []).map(uri => "<li>" + this.displayUri(uri, prefixes, this.uriMaxLength)).join("\n") +
                    "</ul>")
                : '') +
                "</td>"

            ];
            if (!scientificNameFirst) this.inverseArrayValue(rowCols, 1, 2);
            if (!hasAuthor) rowCols[3] = "";
            if (!hasRank) rowCols[4] = "";

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

    download() {
        const hasResults = this.yasr.results && this.yasr.results.json && this.yasr.results.json.results.bindings.length && true;
        if (!hasResults) return undefined;

        return {
            getData: () => this.yasr.results.asCsv(),
            contentType:"text/csv",
            title:"Download result",
            filename:"taxonSearch.csv"
        };
    }

    /* -- Internal functions -- */

    inverseArrayValue(array, index1, index2) {
        let temp1 = array[index1];
        array[index1] = array[index2];
        array[index2] = temp1;
    }

    urnToUrl(uri) {
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

    urnToRdfUrl(uri) {
        if (!uri || !uri.startsWith('urn:')) return uri;

        // Resolve Life science ID (e.g. WoRMS urn, etc.)
        if (uri.startsWith("urn:lsid:")) {
            return "http://www.lsid.info/resolver/api.php?lsid=" + uri;
        }

        return uri;
    }

    simplifyUri(uri, prefixes, truncLength) {
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


    truncText(text, truncLength) {
        // No prefix found: check length
        if (truncLength && truncLength > 3 && text.length > truncLength) {
            return text.substr(0, truncLength-3) + '...';
        }
        return text;
    }

    displayUri(uri, prefixes, textTruncLength) {
        if (!uri) return '';

        let getStartTag;
        if (this.defaults && this.defaults.uriClickTarget) {
            const target = this.defaults.uriClickTarget;
            getStartTag = function(url) {
                return "<a href='" + url + "' target='"+ target +"' >";
            }
        }
        if (this.defaults && this.defaults.onUriClick) {
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

YasrTaxonPlugin.prototype.defaults = {
    uriClickTarget : undefined,
    onUriClick : undefined
};