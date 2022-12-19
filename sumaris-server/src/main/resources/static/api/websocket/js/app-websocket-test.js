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
function AppWebsocketTest() {
    const restBasePath = '/graphql/websocket';
    let websocket;
    let wsUri;
    let output;
    let buttonConnect;
    let inputUri;
    let inputContent;

    const requests_examples = {
        connection_init: {type: "connection_init", "payload": {}},
        account_changes: {id: 1,
            type: "start",
            payload: {
                variables: {
                    pubkey: "5ocqzyDMMWf1V8bsoNhWb1iNwax1e9M7VTUN6navs8of",
                    interval: 10
                },
                extensions: {},
                operationName: "updateAccount",
                query: "subscription updateAccount($pubkey: String, $interval: Int) {updateAccount(pubkey: $pubkey, interval: $interval) {id updateDate}}"
            }
        },
        trip_changes: {
            id: 2,
            type: "start",
            payload: {
                variables: {
                    tripId: 1,
                    interval: 10
                },
                extensions: {},
                operationName: "updateTrip",
                query: "subscription updateTrip($tripId: Int, $interval: Int) {updateTrip(tripId: $tripId, interval: $interval) {id updateDate}}"
            }
        }
    };

    function init() {
        console.debug("Init websocket test app...");

        if (window.location && window.location.origin) {
            wsUri = window.location.origin.replace('http', 'ws') + restBasePath;
        }

        inputUri = document.getElementById("wsUri");
        buttonConnect = document.getElementById("buttonConnect");
        inputUri.value = wsUri;
        inputContent = document.getElementById("content");
        output = document.getElementById("output");

        // Open the socket
        websocket = createWebSocket();
    }

    function createWebSocket()
    {
        wsUri = (inputUri.value) || wsUri;
        var res = new WebSocket(wsUri);
        res.onopen = function(evt) { onOpen(evt) };
        res.onclose = function(evt) { onClose(evt) };
        res.onmessage = function(evt) { onMessage(evt) };
        res.onerror = function(evt) { onError(evt) };
        return res;
    }

    function onOpen(evt)
    {
        log("CONNECTED", "text-muted");

        // Init connection
        doSend(requests_examples.connection_init);

        buttonConnect.innerHTML = "Disconnect";
    }

    function doOpenOrClose()
    {
        if (websocket) {
            websocket.close();
            websocket = undefined;
        }
        else {
            websocket = createWebSocket();
        }
    }

    function onClose(evt)
    {
        if (evt && evt.reason) {
            log("DISCONNECTED - Reason: " + evt.reason, "text-warning");
        }
        else {
            log("DISCONNECTED", "text-warning");
        }

        // Change connection button label
        buttonConnect.innerHTML = "Connect";

        websocket = undefined;
    }

    function onMessage(evt)
    {
        const message = evt.data;
        if (message.indexOf("\"type\":\"error\"") != -1) {
            log('RESPONSE: ' + evt.data, 'text-warning');
        }
        else {
            log('RESPONSE: ' + evt.data, 'text-info');
        }
    }

    function onError(evt)
    {
        log('ERROR: ' + evt.data, 'text-error');
    }

    function doSend(message)
    {
        if (!message) return; // Skip if empty

        if (!websocket) {
            websocket = createWebSocket();
        }
        try {
            // If string, convert twice (to make sure the syntax is correct)
            if (typeof message === "string") {
                message = JSON.stringify(JSON.parse(message));
            }
            // If object, convert to string
            else if (typeof message === "object") {
                message = JSON.stringify(message);
            }

            log("SENT: " + message, "text-muted");
            websocket.send(message);
        }
        catch(error) {
            console.error(error);
            onError({data: (error && error.message || error)});
        }
    }

    function doSendContent() {
        doSend(inputContent.value);
    }

    function log(message, classAttribute)
    {
        const pre = document.createElement("p");
        if (classAttribute) {
            const classes = classAttribute.split(" ");
            for (let i=0; i< classes.length; i++) {
                pre.classList.add(classes[i]);
            }
        }
        pre.style.wordWrap = "break-word";
        pre.innerHTML = message;
        output.appendChild(pre);
    }

    function clearScreen()
    {
        output.innerHTML = "";
    }

    function showExample(name) {
        if (requests_examples[name]) {
            inputContent.value = JSON.stringify(requests_examples[name]);
        }
    }

    window.addEventListener("load", init, false);

    return {
        showExample,
        clearScreen,
        doOpenOrClose,
        doSendContent
    }
}
