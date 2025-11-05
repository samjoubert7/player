const nameFont = '12px sans-serif';
const canvas = document.getElementsByTagName('canvas')[0];
var ctx = canvas.getContext("2d");

var ws = undefined;
var wsError = undefined;
var playerId; // own player object
var players = {};
var playerNames = {};
var playerImg;
var droneImg;
var ctrlPressed = false;
var upPressed = false;
var downPressed = false;
var leftPressed = false;
var rightPressed = false;

function appendOutText(text) {
    document.getElementById('out-text').value += '\n' + text;
}

function reqListener() {
    console.log('Response: ' + this.responseText);
    appendOutText(this.responseText);
}

function handleServerMessage(data) {
    let text = data.data;
    // console.log('Server Message: ' + text);
    if (text.startsWith('$p:')) {
        let content = text.substring('$p:'.length);
        playersMoved(content);
    } else if (text.startsWith('$n:')) {
        let content = text.substring('$n:'.length);
        nameList(content);
    } else if (text.startsWith('$end:')) {
        let content = text.substring('$end:'.length);
        endPlayer(content);
    } else if (text.startsWith('$err:')) {
        let content = text.substring('$err:'.length);
        wsError = content;
    } else if (text.startsWith('$init:')) {
        let content = text.substring('$init:'.length);
        initResp(content);
    } else if (text.startsWith('$send*:')) {
        let content = text.substring('$send*:'.length);
        appendOutText(content);
    } else {
        appendOutText('Unknown response: ' + text);
    }
}

function playersMoved(content) {
    let arr = content.split('\n');
    for (let item of arr) {
        let player = parsePlayer(item);
        playerMoved(player);
    }
}

function playerMoved(player) {
    let img = (player.tp == 'p') ? playerImg : droneImg;
    // Clear previous.
    if (Number.isFinite(player.drawnX) && Number.isFinite(player.drawnY)) {
        if (Number.isFinite(player.nameWidth) && player.nameWidth >= img.naturalWidth) {
            let textX = player.drawnX + ((img.naturalWidth - player.nameWidth) / 2);
            let textY = player.drawnY + ((img.naturalHeight - player.nameHeight) / 2);
            ctx.clearRect(textX, textY, player.nameWidth, player.nameHeight);
        }
        ctx.clearRect(player.drawnX, player.drawnY, img.naturalWidth, img.naturalHeight);
        player.drawnX = undefined;
        player.drawnY = undefined;
    }
    // Was player removed?
    if (player.removed) {
        console.log('Player ' + player.id + ' DELETED');
        players[player.id] = null;
        return;
    }
    if (!Number.isFinite(player.x) || !Number.isFinite(player.y)) {
        console.log('Player ' + player.id + ' move INVALID ' + player.x + ',' + player.y);
        return;
    }
    // Draw new player position.
    console.log('Player ' + player.id + ' moved ' + player.x + ',' + player.y);
    player.drawnX = player.x;
    player.drawnY = player.y;
    // If this is the user's player
    if (player.id == playerId) {
        ctx.fillStyle = 'lightgray';
        ctx.fillRect(player.x, player.y, img.naturalWidth, img.naturalHeight);
    }
    ctx.drawImage(img, player.x, player.y);
    if (player.name) {
        ctx.font = nameFont;
        ctx.fillStyle = 'blue';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(player.name, player.x + (img.naturalWidth / 2), player.y + 1 + (img.naturalHeight / 2));
    }
}

function parsePlayer(playerDesc) {
    let arr = playerDesc.split('\t');
    let player = getPlayer(arr[0]);
    player.x = (arr.length > 0) ? parsePos(arr[1]) : undefined;
    player.y = (arr.length > 1) ? parsePos(arr[2]) : undefined;
    return player;
}

// Number('End') produces NaN but `typeof NaN` yields 'number' !
function parsePos(text) {
    if (!text || text.length == 0) {
        return undefined;
    }
    let value = Number(text);
    // convert NaN to undefined
    return Number.isFinite(value) ? value : undefined;
}

function getPlayer(id) {
    let player = players[id];
    if (!player) {
        let tp = id.substring(0, 1);
        let auto = tp == 'd';
        player = {id: id, tp: tp, auto: auto};
        players[id] = player;
    }
    return player;
}

function nameList(content) {
    let arr = content.split('\n');
    for (let item of arr) {
        let index = item.indexOf(':');
        if (index > 0) {
            let id = item.substring(0, index);
            let name = item.substring(index + 1);
            let player = getPlayer(id);
            console.log('Player ' + id + ' -> ' + name);
            updatePlayerName(player, name);
        }
    }
}

function endPlayer(id) {
    let player = getPlayer(id);
    player.removed = true;
    playerMoved(player);
}

function updatePlayerName(player, name) {
    if (player.name != name) {
        player.name = name;
        ctx.font = nameFont;
        player.textWidth = (name) ? ctx.measureText(name).width : undefined;
        player.textHeight = 12;
    }
}

function initResp(content) {
    console.log('INIT: ' + content);
    let width;
    let height;
    let arr = content.split('\t');
    for (let item of arr) {
        let index = item.indexOf(':');
        if (index > 0) {
            let id = item.substring(0, index);
            let value = item.substring(index + 1);
            if (id == 'w') {
                width = parsePos(value);
            } else if (id == 'h') {
                height = parsePos(value);
            } else if (id == 'id' && value.length > 0) {
                // if previous player id exists
                if (playerId && playerId != value) {
                    players[value] = undefined;
                }
                // new player id
                playerId = value;
                document.getElementById('player-id').value = value;
            }
        }
    }
    if (Number.isFinite(width) && Number.isFinite(height)) {
        if (width != canvas.width && height != canvas.height) {
            console.log('Resize canvas ' + width + ', ' + height);
            canvas.width = width;
            canvas.height = height;
            // re-get canvas after size change
            ctx = canvas.getContext("2d");
        }
    }
}

function getInitName() {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', 'http://localhost:8080/init-name');
    xhr.onload = function() {
        // Check if the request was successful (HTTP status code 200-299)
        if (xhr.status >= 200 && xhr.status < 300) {
            console.log('Player name response:', xhr.responseText);
            document.getElementById('player-name').value = xhr.responseText;
        } else {
            // Handle HTTP errors (e.g., 404 Not Found, 500 Internal Server Error)
            console.error('Request failed with status:', xhr.status);
        }
    };
    xhr.onerror = function() {
        console.error('Network error occurred during the request.');
    };
    xhr.send();
    console.log('Sent init-name req');
}

function connect() {
    if (ws) {
        return;
    }
    let chatName = document.getElementById('player-name').value;
    if (!chatName || chatName.length == 0) {
        appendOutText('Must set name before connect');
        return;
    }
    console.log('Connect');
    ws = new WebSocket('ws://localhost:8080/ws/notifications?name=' + chatName);
    ws.onmessage = handleServerMessage;
    ws.onopen = function() {
        announce('Connected');
        userDisabled();
        // setTimeout(sendName, 50);
    }
    ws.onclose = function() {
        if (wsError) {
            announce('FAILURE, ' + wsError);
            wsError = undefined;
        } else {
            announce('Disconnected!!!');
        }
        if (playerId) {
            endPlayer(playerId);
        }
        ws = undefined;
    }
    // setConnected(true);
    announce("Connecting");
}

function disconnect() {
    if (ws) {
        console.log('Disconnect');
        ws.close();
        ws = undefined;
    } else {
        console.log('Disconnect IGNORED');
    }
    // setConnected(false);
    announce("Disconnecting");
    userDisabled();
    if (playerId) {
        endPlayer(playerId);
    }
}

function sendInit() {
    if (ws) {
        ws.send('$init:');
        console.log("Sent init request");
    } else {
        console.log("Send IGNORED");
    }
}

function sendName() {
    let chatName = document.getElementById('player-name').value;
    if (ws) {
        ws.send('$name:' + chatName);
        console.log("Sent: " + chatName);
    } else {
        console.log("Send IGNORED");
    }
}

function keyDownHandler(e) {
    ctrlPressed = e.ctrlKey;
    keyUpDownHandler(e, true);
}

function keyUpHandler(e) {
    ctrlPressed = e.ctrlKey;
    keyUpDownHandler(e, false);
}

function keyUpDownHandler(e, value) {
    let arrowKey = false;
    switch (e.key) {
        case "Up":
        case "ArrowUp":
            upPressed = value;
            arrowKey = true;
            if (!ctrlPressed) {
                e.preventDefault();
            }
            break;
        case "Down":
        case "ArrowDown":
            downPressed = value;
            arrowKey = true;
            if (!ctrlPressed) {
                e.preventDefault();
            }
            break;
        case "Left":
        case "ArrowLeft":
            leftPressed = value;
            arrowKey = true;
            if (!ctrlPressed) {
                e.preventDefault();
            }
            break;
        case "Right":
        case "ArrowRight":
            rightPressed = value;
            arrowKey = true;
            break;
        default:
            return;
    }
    if (!arrowKey) {
        return;
    }
    if (!ctrlPressed) {
        e.preventDefault();
    }
    let text = '';
    if (upPressed) {
        text += 'U';
    } else if (downPressed) {
        text += 'D';
    }
    if (leftPressed) {
        text += 'L';
    } else if (rightPressed) {
        text += 'R';
    }
    if (ws) {
        ws.send('$key:' + text);
    }
}

function userEnabled() {
    document.getElementById('player-id').disabled = false;
}

function userDisabled() {
    document.getElementById('player-id').disabled = true;
}

function showStatus(msg) {
    document.getElementById('status').value = msg;
}

function announce(msg) {
    showStatus(msg);
    appendOutText(msg);
}

function postInit() {
    console.log('postInit');
    try {
        let newX = undefined;
        let newY = undefined;
        let item = {id: 'W01', x: 24, y: 48};
        let itemMap = {};
        console.log('Undefined map indexed: ' + JSON.stringify(itemMap['f8']));
        itemMap[item.id] = item;
        console.log('Defined indexed: ' + JSON.stringify(itemMap[item.id]));
        item.x = newX;
        item.y = newY;
        console.log('Updated indexed: ' + JSON.stringify(itemMap[item.id]));
        itemMap[item.id] = undefined;
        console.log('Deleted indexed: ' + JSON.stringify(itemMap[item.id]));
        newX = 0;
        if (newX) {
            console.log('value 0 is not undefined');
        } else {
            console.log('value 0 is evaluated as undefined');
        }
        let otherValue = false;
        if (otherValue >= 0) {
            console.log('(false >= 0) returned true');
        } else {
            console.log('(false >= 0) returned false');
        }
        otherValue = "aaa";
        if (otherValue >= 0) {
            console.log('("aaa" >= 0) returned true');
        } else {
            console.log('("aaa" >= 0) returned false');
        }
        otherValue = Number("End")
        console.log('Number("End") returned ' + otherValue);
        console.log('typeof Number("End") returned ' + (typeof otherValue));
        let result = (typeof otherValue === 'number');
        console.log('(typeof Number("End") === "number") returned ' + result);
        otherValue = 0;
        result = (typeof otherValue === 'number');
        console.log('(typeof 0 === "number") returned ' + result);
        otherValue = undefined;
        result = (typeof otherValue === 'number');
        console.log('(typeof undefined === "number") returned ' + result);
        otherValue = 0;
        result = (otherValue == 'End');
        console.log('(0 == "End") returned ' + result);
        // Number.isFinite(
        let value = parsePos('0');
        console.log('parsePos("0") -> ' + value);
        console.log('Number.isFinite(parsePos("0")) -> ' + Number.isFinite(value));
        value = parsePos();
        console.log('parsePos() -> ' + value);
        value = parsePos(undefined);
        console.log('parsePos(undefined) -> ' + value);
        console.log('Number.isFinite(parsePos(undefined)) -> ' + Number.isFinite(value));
        value = parsePos('');
        console.log('parsePos("") -> ' + value);
        value = parsePos('End');
        console.log('parsePos("End") -> ' + value);
        console.log('Number.isFinite(parsePos("End")) -> ' + Number.isFinite(value));
    } catch (e) {
        console.log('postInit exception: ' + JSON.stringify(e));
    }
}

function init() {
    playerImg = document.getElementById('player-img');
    droneImg = document.getElementById('drone-img');
    // document.getElementById('go-btn').onclick = goBtnClicked;
    document.getElementById('conn-btn').onclick = connect;
    document.getElementById('disc-btn').onclick = disconnect;

    document.addEventListener("keydown", keyDownHandler, true);
    document.addEventListener("keyup", keyUpHandler, true);
    
    setTimeout(getInitName, 50);
    setTimeout(postInit, 150);
}

window.onload = init;
window.onbeforeunload = disconnect;
