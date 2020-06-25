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
function AppMenu(menuId, opts) {

    const indexUrl = "/api";

    function load() {
        if (!menuId) {
            console.debug("[menu] No div menu id. Skipping menu creation.");
            return; // Skip
        }

        console.debug("[menu] Loading menu...");

        const afterMenuLoaded = () => {

            // Expand the menu
            if (!opts || opts.expand !== false) {
                $(menuId + ' .navbar, ' + menuId+'.navbar').addClass('navbar-expand-lg');
            }

            if (!opts || opts.fixed !== false) {
                $(menuId + ' .navbar, ' + menuId+'.navbar').addClass('fixed-top');
                $('body').css({ paddingTop: '70px'});
            }

            if (opts && opts.active) {
                $(menuId + ' ' + opts.active).addClass('active');
            }

                // Load the title
            loadTitle();

            // Load items
            loadItems();

        };

        if (!$(menuId + ' .menu-title').length) {
            console.debug("[menu] Loading content from {"+ indexUrl +"}...");
            $(menuId).load(indexUrl + ' #menu', null, afterMenuLoaded);

        }
        else {
            afterMenuLoaded();
        }
    }

    function loadNodeInfo(successCallback) {
        $.ajax({
            url: "/api/node/info/",
            cache: true,
            dataType: 'json',
            success: (res) => {
                if (successCallback) successCallback(res);
            }
        });
    }

    function loadTitle(divId, successCallback) {
        divId = divId || (menuId + ' .menu-title');

        if (!opts || opts.title !== false || successCallback) {
            console.debug("[menu] Loading title...");
            loadNodeInfo((res) => {
                    const title = res && res.nodeLabel && (res.nodeLabel + ' Pod Api') || 'Pod API';
                    $(divId).html(title);
                    console.debug("[menu] Loading title [OK]");
                    if (successCallback) successCallback(res);
                });
        }
        else {
            $(divId).html(''); // Remove loading text
            if (successCallback) successCallback();
        }
    }

    function loadItems(silent) {

        if (!silent) console.debug("[menu] Loading items...");

        // Check if doc links are loaded
        if (!$('#ul-documentation').length) {
            // Add an invisible div, to load items
            $('body').append('<div class="d-none" id="ul-documentation"></div>');
            $('#ul-documentation').load(indexUrl + ' #ul-documentation', null, () => loadItems(true));
            return;
        }

        // Check if tools links are loaded
        const docItems = $('#ul-tools');
        if (!docItems.length) {
            // Add an invisible div, to load items
            $('body').append('<div class="d-none" id="ul-tools"></div>');
            $('#ul-tools').load(indexUrl + ' #ul-tools', null, () => loadItems(true));
            return;
        }

        // Fill documentation
        $(menuId + ' .menu-documentation').html(''); // remove loading text
        $('#ul-documentation a').clone().each((index, ele) => {
            const element = $(ele);
            element.addClass('dropdown-item');
            element.appendTo(menuId + ' .menu-documentation');
        });

        // Fill tools
        $(menuId + ' .menu-tools').html(''); // remove loading text
        $('#ul-tools a').clone().each((index, ele) => {
            const element = $(ele);
            element.addClass('dropdown-item');
            element.appendTo(menuId + ' .menu-tools');
        });

        console.debug("[menu] Loading items [OK]");
    }

    function goHome() {
        window.open(indexUrl, '_self');
    }

    window.addEventListener("load", load, false);

    const exports = {
        load,
        loadNodeInfo,
        loadTitle,
        goHome
    };

    return exports;
}
