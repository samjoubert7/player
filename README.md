The Player project demonstrates Spring WebSocket support with a boring game where multiple users and drones move about an arena,
bouncing off each other when they collide.  This project also demonstrates a few multi-threading concepts.

The UI is in a browser so that multiple browser windows can be opened side-by-side to see co-ordinated action.  It is implemented in plain Javascript with minimal styling.  Use a custom minimalist message format because there can be a lot of data.

See file `Design.md` for a detailed software design.

Using the app:
1. Start the app.
2. Steps 3 to 7 can be repeated multiple times.
3. Load page `http://localhost:8080/` in browser.
4. Change 'Name' if you want. Keep the name to 3 characters.
5. Click button `Connect`.
6. Use the arrow keys to move your own player.
7. Click `Disconnect` when finished.
8. Close the app.

If the app is closed while users are connected then their browser sessions will have status `Disconnected!!!`.