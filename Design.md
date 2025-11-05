The Player project demonstrates Spring WebSocket support with a boring game where multiple users and drones move about a game board, bouncing off each other when they collide.  This project also demonstrates a few multi-threading concepts.

The game board is in a browser so that multiple browser windows can be opened side-by-side to see co-ordinated action.

The state of the game board is maintained on the back-end server, with the movements of each user's game piece sent to the server
where the back-end detects collisions and communicates movements back to the browser windows.  Users must move their game pieces
while drones are constantly in motion.

Each browser instance opens its own Web Socket connection to the server and all communication takes place over it.

# Web Socket Overview
Before web sockets the browser always requested data from the server but the server had no way to communicate with the browser.
The browser can do long-polling but sometimes the application needs real 2-way data flow.

A web socket is a TCP socket between browser and server which allows 2-way communication. The web socket functionality is supported natively by most browsers nowadays.  Both server and browser quickly detect if the link is down.  The browser can still use normal HTTP requests while the web socket is active.

The browser is responsible for initiating the web socket connection.  The web server maintains the set of web socket sessions and it needs to know which session to send data out on.

A web socket can handle only 1 message at a time in each direction.  The browser implementation is single-threaded but the server may well have multiple threads that can send messages on the same socket at the same time -- these must be co-ordinated so the messages are sent sequentially.

# Components

A web server hosts the game board with its players and drones.

Each user opens the web page to load a static view of the HTML, Javascript and CSS.

# Implementation Overview

The web server is implemented as a Spring Boot app with an embedded Tomcat web server, and includes the Spring WebSocket libraries.

The web page is implemented in static HTML 5 with native JavaScript and Bootstrap CSS.

The browser interface is just the UI, all the game logic is implemented on the server.

The JavaScript uses the browser's native websocket support.  The updates between UI and server are all communicated over the websocket.

# Software Component Overview

The server maintains the game board with its players and drones.  The user controls the movement of their own player with arrow keys and each keypress is communicated to the server.  The locations of the players and drones are sent to each browser instance so they can be displayed.

The game board receives the player moves and has a timer to move each player and drone at regular intervals, detecting collisions and making them appear to bounce.

The server maintains a websocket session per browser instance.


# Sequence Of Events
1. User loads the webpage in the browser.
2. The browser requests a random username from the server and the user can modify it.
3. User clicks the connect button and the browser opens a new websocket.
4. The server receives the request and establishes the websocket connection.
5. The server creates a new player object and adds it to the board.
6. The server starts a timer to make the next moves periodically.
7. After each move, send the updated player and drone locations to all the websockets.
8. User clicks the disconnect button to end involvement with the game.
9. The disconnected user's icon is removed from all the other user screens.
10. When the last user has disconnected, stop the timer so it doesn't make any more moves.

# Software Components

## Board
This structure contains the board dimensions, the set of player objects, and the code to move the pieces and detect collisions.

## Player
Each player object represents a moving game piece on the board.

## Player Service

The Player Service co-ordinates the other components. Its functions:
1. The board and player initializations.
2. Receive user move keypresses and action them.
3. Manage the main game timer function to trigger the next round of moves.
4. Communicate game board updates to the browser interfaces.

## WebSocketConfig
This is the configuration object which sets up the various objects and finally registers the Web Socket handler and a handshake interceptor.

## PlayerWebSocketHandler
The Web Socket handler has a number of responsibilities:
1. Receive the web socket connect requests from the browser and maintain a separate WebSocketSession for each one.
2. Handle WebSocketSession tear-down if the web socket is closed by either browser or server app exit.
3. Handle incoming messages from a browser over a WebSocketSession.
4. Route outgoing messages to the required WebSocketSession(s).

## PlayerWebSocketSession
Each web socket has its own PlayerWebSocketSession instance which manages the Spring WebSocketSession.

The function to send a message will first obtain the message lock before sending.  A new virtual thread is started for each message send which can wait until the lock is obtained.

The PlayerWebSocketSession also participates in the socket shutdown sequence to ensure the last message is sent before closing the session.  The websocket close sequence will wait until the last message has been delivered before shutting down the web socket.

## PlayerHandshakeInterceptor
When the browser connects the websocket the user name is added as an HTTP parameter.  For the server to get access to this parameter it must register a handshake interceptor at startup.
The startup sequence:
1. The Spring framework receives the websocket connect request and establishes the TCP connection.
2. The interceptor reads the HTTP parameters and copies the user name parameter to the websocket session.
3. The PlayerWebSocketHandler afterConnectionEstablished entry-point creates a new PlayerWebSocketSession to contain the WebSocketSession, and saves it in the session map.
4. Create the new player.
5. Start the animation thread if not already active.

# Messaging

A custom message format is used which reduces the volume of data going through each websocket in case many players are added.

Each message starts with a header to indicate the message type and the format of the data depends on the type.

The message formats below do NOT contain any space characters -- tab and newline characters are used as delimiters because those are not allowed to appear in id's.

The player-id is an internal id, while the player name is what is shown on-screen.  The player name doesn't change, so it isn't included in the player location updates, but it needs to be communicated to all the browser instances when a player is added.

The browser saves the latest location for each player and drone so that it can erase the icon from the screen when the next location change arrives or the player is removed.

### Message Types

#### Player Location:
Sent to browser when a player or drone is moved.
`$p: <id> <tab> <x> <tab> <y>`

Example:
`p:p23<tab>11<tab>238`

#### Player removed:
Sent to browser when a player's websocket disconnects.
`$end: <id>`

Example:
`end:p23`

#### Player keypress:
Sent to server when player presses the up, down, left or right arrow keys in the browser.
The up or down keys can be paired with the left or right keys for diagonal motion.
It is valid to send a message without any arrow keys on the last key-up event so the player stops moving.
`$key: <U> <D> <L> <R>`

For example, if a player presses the left an up arrow keys simultaneously then the message would be
`$key:LU`

The browser needs to swallow the arrow key with `event.preventDefault()` so that the browser window doesn't scroll as well.  The user can do Ctrl-arrowKey combination to scroll the browser window without moving the player on-screen.

#### Player name:
Sent to all browsers when a player joins.
`$n: <id> : <name> <newline> <id> : <name> <newline> ...`

The player name is displayed on the player icon to help the users identify who's who.

#### Initialization:
Sent to browser when a player joins.
The browser adjusts the size of the arena to match the width and height.
The browser saves the player-id which was generated by the server when the player was created.
`$init: w: <width> <tab> h: <height> <tab> id: <player-id>`

Example:
`$init:w:600<tab>h:400<tab>id:p23`

#### Error status:
Sent to browser if any error happens so it can display it in the status box.
`$err: <message>`

# Threading Model

This app uses virtual threads for everything.

A separate virtual thread is started for sending out data on each Web Socket.  When that is done, wait for each virtual thread to complete before starting the next timer thread.  My experience is that virtual threads are NOT designed to be long-running.

The main timer thread could have been a normal (platform) scheduled thread but that requires a second thread executor.  It is easy enough to implement the thread delay and reschedule for virtual threads.

# Synchronization

The board data structures are all synchronized on the same lock.

## Javascript Implementation Details

The position of each player and drone needs to be saved so it can be erased before drawing it at the new location.  There are some corner cases where the icon hasn't been drawn and then the saved location needs to be marked as invalid so we don't erase an already-drawn icon.  The X and Y locations can also be outside the canvas size a little, meaning they can be less than zero down to `- iconSize`.

The invalid X and Y variables are set to `undefined` but the check for valid values has to be `if (Number.isFinite(variable))` so that it returns true if the variable is set to numeric value zero.  The `if (variable)` returns false for a zero value.

My itty-bitty development laptop isn't performant enough for TypeScript development, but this Javascript code is small enough that a bit of developer discipline means I can get away without the type safety checks.

## Random Number Generator

The app needs a steady supply of pseudo-random numbers. The Marsaglia XORWOW generator is used which has the following advantages:
1. Produces numbers quickly with XOR and bit shift operations.
2. Produces numbers with excellent statistical properties.
3. Produces 2^123 numbers before the sequence repeats.

This pseudo-random number generator is NOT designed for cryptographic use but that is of no concern here.

Most CPUs have barrel-shifters which means they can left-shift or right-shift a number by any number of bits in the same amount of time.  Without a barrel-shifter the CPU has to repeat a single bit-shift operation multiple times, increasing the execution time.
