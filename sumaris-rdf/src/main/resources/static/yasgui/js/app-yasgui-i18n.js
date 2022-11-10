/*-
 * #%L
 * SAR :: RDF features
 * %%
 * Copyright (C) 2018 - 2021 Service d'Administration des Référentiels marins (SAR)
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
var translations = {
    EXAMPLE_DOTS: "Exemples : ",
    PREFIXES_DOTS: "Préfixes : ",
    PREFIXES_GROUP: {
        COMMON: 'Commun',
        TAXON: 'Taxons',
        ORGANIZATION: 'Interlocuteurs'
    }
}

function i18n(key, params) {
    var keys = key.split('.');
    var value = translations;

    for (var i = 0; i<keys.length; i++) {
        value = value[keys[i]];
        if (!value) return key;
    }
    // Not found: return the key
    if (!(typeof value === 'string')) return key;

    // Replace params
    if (params) {
        Object.keys(params).forEach(paramKey => {
            const paramValue = params[paramKey];
            value = value.replaceAll('{{' + paramKey + '}}', paramValue);
        })
    }

    return value;
}
