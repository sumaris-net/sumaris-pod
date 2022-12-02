/*-
 * #%L
 * SUMARiS:: Server
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
// Workaround to add Math.trunc() if not present - fix #144
if (Math && typeof Math.trunc !== 'function') {
    console.debug("[utils] Adding Math.trunc() -> was missing on this platform");
    Math.trunc = (number) => {
        return parseInt((number - 0.5).toFixed());
    };
}

// Workaround to add "".format() if not present
if (typeof String.prototype.format !== 'function') {
    console.debug("[utils] Adding String.prototype.format() -> was missing on this platform");
    String.prototype.format = function() {
        const args = arguments;
        return this.replace(/{(\d+)}/g, function(match, number) {
            return typeof args[number] != 'undefined' ? args[number] : match;
        });
    };
}

// Useful function
function AppUtils() {

    const PHRASE_SEPARATOR_REGEXP = new RegExp(/"([^"]+)"/g);
    const WORD_SEPARATOR_REGEXP = new RegExp(/[",;\t\s]+/);

    function capitalize(value) {
        return value.substr(0, 1).toUpperCase() + this.substr(1);
    }

    function changeCaseToUnderscore(value) {
        if (!value) return value;
        return value.replace(/([a-z])([A-Z])/g, '$1_$2').toLowerCase();
    }

    function parseLocationHash() {
        const result = {};
        if (!window.location || !window.location.hash || window.location.hash.length === 1) return;

        const hashParts = (window.location.hash || '#').substr(1).split('&');
        (hashParts || []).forEach(param => {
            const paramParts = param.split('=', 2);
            const paramName = paramParts[0];
            if (paramName.trim().length) {
                const paramValue = decodeURIComponent(paramParts[1] || true);
                result[paramName.trim().toLowerCase()] = paramValue;
            }
        });

        console.debug('[utils] Hash parameters: ', result);

        return result;
    }

    function setInputValue(id, value, isCheckbox) {
        const ele = document.getElementById(id);
        if (!ele) {
            console.error('Cannot found element #' + id);
            return;
        }
        if (isCheckbox) {
            ele.checked = value !== false;
        }
        else if (ele.value !== undefined) {
            ele.value = value;
        }
    }

    function getInputValue(id, isCheckbox) {
        const ele = document.getElementById(id);
        if (!ele) {
            console.error('Cannot found element #' + id);
            return;
        }
        if (isCheckbox) {
            return ele.checked;
        }
        return ele.value;
    }

    function getSearchTerms(id) {
        const value = (getInputValue(id) || '').trim();

        // Add phrases
        const matches = PHRASE_SEPARATOR_REGEXP.exec(value);
        const terms = matches && matches.slice(1) || [];

        return terms.concat(value
          .replace(PHRASE_SEPARATOR_REGEXP, '') // Remove phrase (already added)
          .replace(/[\s]*([+*])[\s]*/g, '$1') // Remove space between '+' or '*'
          .split(WORD_SEPARATOR_REGEXP)
          .map(s => s.trim())
          .filter(s => s.length > 0))
          .map(s => s.replace(/[*+]+/g, '*'))
          ;
    }

    // Exports
    return {
        capitalize,
        changeCaseToUnderscore,
        parseLocationHash,
        getInputValue,
        setInputValue,
        getSearchTerms
    };
}

