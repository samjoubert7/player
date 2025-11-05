package samj.player.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import samj.player.model.Player;
import samj.player.service.PlayerService;
import samj.player.util.JacksonMapper;
import samj.player.util.PlayerUtil;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@CommonsLog
@RequiredArgsConstructor
public class PlayerWebSocketHandler implements WebSocketHandler, ApplicationListener<ContextClosedEvent> {
    private final ExecutorService virtualThreadExecutor;
	private final JacksonMapper mapper;
	private final PlayerService playerService;
	private final AtomicInteger maxDataSize = new AtomicInteger(8192);

	private final Map<String, PlayerWebSocketSession> sessions = new ConcurrentHashMap<>(16, 0.75f, 1);

	private static final String MSG_INIT_PREFIX = "$init:";
	private static final String MSG_ERROR_PREFIX = "$err:";
	private static final String MSG_UNKNOWN_PREFIX = "$?:";
	private static final String MSG_SEND_ALL_PREFIX = "$send*:";
	private static final String MSG_KEY_PREFIX = "$key:"; // arrow key pressed on browser.

	@Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        // var principal = session.getPrincipal();
        String id = session.getId();
        String name = getPlayerName(session);
        PlayerWebSocketSession customSession = new PlayerWebSocketSession(name, session, virtualThreadExecutor);
        log.info("WebSocket " + id + " Opened " + name);
        sessions.put(id, customSession);
        checkDataSize(session);
		try {
			// setupPlayer can throw exception if name clashes, execute now.
	        setupPlayer(customSession);
	        // sendSetup must run a bit later after connection fully up.
            virtualThreadExecutor.execute(() -> sendSetup(customSession));
		} catch (InterruptedException e) {
			log.error("Player add aborted, session id: " + session.getId());
		} catch (IllegalArgumentException e) {
			log.error("Player exception: " + e.getMessage());
			customSession.setDelayedClose(true);
			customSession.send(MSG_ERROR_PREFIX + e.getMessage());
		}
    }

	@Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // var principal = session.getPrincipal();
        String id = session.getId();
        log.info("WebSocket " + id + " Closed " + WsCloseStatus.toString(status));
        PlayerWebSocketSession customSession = sessions.remove(id);
        if (customSession != null) {
        	String name = customSession.getPlayerName();
        	sendAll(MSG_SEND_ALL_PREFIX + "[" + name + "]:<exit>");
        }
        virtualThreadExecutor.execute(() -> endPlayer(customSession));
    }
    
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
    	log.info("Shutdown, close all websocket sessions");
    	List<PlayerWebSocketSession> sessionList = new ArrayList<>(sessions.values());
    	for (PlayerWebSocketSession session : sessionList) {
    		String name = session.getPlayerName();
    		sendAll(MSG_SEND_ALL_PREFIX + "[" + name + "]:<exit>");
    	}
    	for (PlayerWebSocketSession session : sessionList) {
    		try {
				session.close();
			} catch (IOException e) {
				log.error("Closing session " + session.getSessionId() + ": " + e.getMessage());
			}
    	}
    }

    private String getPlayerName(WebSocketSession session) {
    	Object obj = session.getAttributes().get("playerName");
    	return (obj != null) ? obj.toString() : null;
    }

    private void checkDataSize(WebSocketSession session) {
    	int newMaxSize = session.getTextMessageSizeLimit();
    	maxDataSize.getAndUpdate(value -> Math.min(value, newMaxSize));
    	log.info("maxDataSize: " + maxDataSize.get());
    }
    
    public int getMaxDataSize() {
    	return maxDataSize.get();
    }

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		if (message instanceof TextMessage textMessage) {
			handleTextMessage(session, textMessage);
			
		} else if (message instanceof BinaryMessage binaryMessage) {
			handleBinaryMessage(session, binaryMessage);
			
		} else if (message instanceof PongMessage pongMessage) {
			handlePongMessage(session, pongMessage);
			
		} else {
			String id = session.getId();
			Object payload = message.getPayload();
			throw new IllegalStateException("WebSocket " + id + 
					" Unexpected message type: " + payload.getClass().getName() + 
					" => " + mapper.toString(payload));
		}
	}

	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String id = session.getId();
        // log.info("WebSocket " + id + " Text " + message);
    	PlayerWebSocketSession customSession = sessions.get(id);
        String response = null;
        String text = message.getPayload();
        int index = text.indexOf(':');
        String prefix = (index > 0) ? text.substring(0, index + 1) : text;
        String content = (index > 0 && index < text.length()) ? text.substring(index + 1) : "";
        switch (prefix) {
	        case MSG_SEND_ALL_PREFIX -> {
	        	String sender = customSession.getPlayerName();
	        	sendAll(MSG_SEND_ALL_PREFIX + "[" + sender + "]:" + content);
	        }
	        case MSG_KEY_PREFIX -> {
	        	String playerId = customSession.getPlayerId();
	        	playerService.playerKey(playerId, content);
	        }
	        default -> {
	        	response = MSG_UNKNOWN_PREFIX + text;
	            customSession.send(response);
	        }
        }
	}

	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        String id = session.getId();
        String text = PlayerUtil.toHex(message.getPayload(), message.getPayloadLength());
        log.info("WebSocket " + id + " Binary " + text);
	}

	protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        String id = session.getId();
        String text = PlayerUtil.toHex(message.getPayload(), message.getPayloadLength());
        log.info("WebSocket " + id + " Pong " + text);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String id = session.getId();
        log.info("WebSocket " + id + " Exception " + exception.getMessage());
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}

	private void setupPlayer(PlayerWebSocketSession session) throws InterruptedException {
		Player player = playerService.addPlayer(session);
		session.setPlayerId(player.getId());
		log.info("WebSocket " + session.getSessionId() + " Player id: " + player.getId());
	}
	
	private void endPlayer(PlayerWebSocketSession session) {
		String id = session.getPlayerId();
		if (id == null) {
			// player not added, nothing more to do
			return;
		}
		try {
			playerService.removePlayer(id);
			session.setDelayedClose(true);
			sendAll("$end:" + id);
		} catch (InterruptedException e) {
			log.error("Interrupted send player end");
		}
	}

	private void sendSetup(PlayerWebSocketSession customSession) {
		try {
			// Wait for websocket connection up.
			Thread.sleep(1);
			// Send board init.
	        String playerId = customSession.getPlayerId();
	        String init = playerService.getInit(playerId);
	        customSession.send(MSG_INIT_PREFIX + init);
	        // Send player name updates
	        String allPlayerNames = getAllPlayerNames();
	        customSession.send("$n:" + allPlayerNames);
	        Player player = playerService.getPlayer(playerId);
	        sendAll("$n:" + player.getId() + ":" + player.getName());
		} catch (InterruptedException e) {
			log.error("Send setup interrupted");
		}
	}

	private String getAllPlayerNames() {
		StringBuilder sb = new StringBuilder(128);
		for (Player p : playerService.getPlayerList()) {
			if (p.isAuto()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append(p.getId()).append(':').append(p.getName());
		}
		return sb.toString();
	}

	// Initiate sending to all sessions, and then wait for each one to complete.
	public void sendAll(String message) {
		List<Future<?>> futures = new ArrayList<>(sessions.size());
		for (PlayerWebSocketSession customSession : sessions.values()) {
			String name = customSession.getPlayerName();
			if (name != null && !name.isBlank()) {
				futures.add(customSession.send(message));
			}
		}
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				log.info("Error waiting for message send: " + e.getMessage());
			}
		}
	}

	public void send(String sender, String content) {
		sendAll(MSG_SEND_ALL_PREFIX + "[" + sender + "]:" + content);
	}
}
