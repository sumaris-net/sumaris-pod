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
    const menuHeight = opts && opts.height || '70px';

    function init() {
        if (!menuId) {
            console.debug("[menu] No div menu id. Skipping menu creation.");
            return; // Skip
        }

        const navbarSelector = [menuId + ' .navbar', menuId+'.navbar'].join(', ');

        console.debug("[menu] Loading menu...");

        const afterMenuLoaded = () => {
            const navBar = $(navbarSelector);
            if (!navBar.length) return; // Menu not loaded

            // Expand the menu
            if (!opts || opts.expand !== false) {
                navBar.addClass('navbar-expand-lg');
            }

            if (!opts || opts.fixed !== false) {
                navBar.addClass('fixed-top');
                $('body').css({ paddingTop: 'var(--menu-height, ' + menuHeight +')'});
            }

            if (opts && opts.active) {
                $(menuId + ' ' + opts.active).addClass('active');
            }

            // Load the title
            loadTitle();

            // Load items
            loadItems();

            initMenu();

        };

        // Load menu content, if need
        if (!$(navbarSelector).length) {
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

        if (!opts || !opts.title || successCallback) {
            console.debug("[menu] Loading title...");
            loadNodeInfo((res) => {
                const title = res && res.nodeLabel && (res.nodeLabel + ' Pod Api') || 'Pod API';

                $(divId).html(title);
                console.debug("[menu] Loading title [OK]");
                if (successCallback) successCallback(res);
            });
        }
        else {
            $(divId).html(opts.title || '');
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
        const docMenu = $(menuId + ' .menu-documentation');
        docMenu.html(''); // remove loading text
        $('#ul-documentation a, #ul-documentation .divider').clone().each((index, ele) => {
            const element = $(ele);
            if (element.hasClass('divider')) {
                // Add divider
                if (index !== 0) {
                    docMenu.append('<div class="dropdown-divider"></div>');
                }
                // Add header
                element.addClass('dropdown-header');
                element.appendTo(docMenu);
            }
            else {
                element.addClass('dropdown-item');
                element.appendTo(docMenu);
            }
        });

        // Fill tools
        const toolsMenu = $(menuId + ' .menu-tools');
        toolsMenu.html(''); // remove loading text
        $('#ul-tools a, #ul-tools .divider').clone().each((index, ele) => {
            const element = $(ele);

            if (element.hasClass('divider')) {
                // Add divider
                if (index !== 0) {
                    toolsMenu.append('<div class="dropdown-divider"></div>');
                }
                // Add header
                element.addClass('dropdown-header');
                element.appendTo(toolsMenu);
            }
            else {
                element.addClass('dropdown-item');
                element.appendTo(toolsMenu);
            }

        });

        console.debug("[menu] Loading items [OK]");
    }

    function goHome() {
        window.open(indexUrl, '_self');
    }


    function initMenu() {
        console.info('[app] Init menu behavior');

        const mobile = window.matchMedia("(max-width: 767px)").matches;
        const url = window.location.pathname;
        // create regexp to match current url pathname and remove trailing slash if present as it could collide with the
        // link in navigation in case trailing slash wasn't present there
        const urlRegExpStr = url === '/'
          ? window.location.origin + '/?$'
          : url.replace(/\/$/,'').replace(/\.html$/,'')
          + '(\.html)?$';
        const urlRegExp = new RegExp(urlRegExpStr);
        console.info('[menu] Detecting active menu item, for url: ' + url, urlRegExpStr);

        $('.navbar-nav li.nav-item a.nav-link').each((i, item) => {
            // and test its normalized href against the url pathname regexp
            if(item.href && item.href !== '#' && urlRegExp.test(item.href.replace(/\/$/,''))){
                $(this).addClass('active');
            }
        });
        // Same, for dropdown menu (should enable root nav-item, when a child dropdown-item is active
        $('.navbar-nav li.nav-item.dropdown').each(function(){
            const rootNavItem = this;
            $(this).children('.dropdown-menu').each(function(){
                $(this).children('a.dropdown-item').each(function(){
                    if (this.href && this.href !== '#' && urlRegExp.test(this.href.replace(/\/$/,''))){
                        $(this).addClass('active');
                        $(rootNavItem).addClass('active');
                    }
                });
            })

        });

        if (!mobile) {
            $('.dropdown-show-hover').hover(function(event) {
                  console.debug('[menu] Hover dropdown item', event.target);
                  $(this).addClass('show');
                  $(this).children('.dropdown-menu').addClass('show');
              },
              function() {
                  $(this).removeClass('show');
                  $(this).children('.dropdown-menu').removeClass('show');
              });
        }
        else {
            $('.dropdown-show-hover>a').click(function(event) {
                console.debug('[menu] Click dropdown item', event.target);
                $(this).addClass('show');
                var children = $(this).children('.dropdown-menu');
                if (children.length) {
                    children.addClass('show');
                }
            });
        }
    }

    $(document).ready(() => init());

    const exports = {
        load: init,
        loadNodeInfo,
        loadTitle,
        goHome
    };

    return exports;
}
