package samj.player.ws;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

@RequiredArgsConstructor
@EqualsAndHashCode
@CommonsLog
public class PlayerWebSocketSession {

	@Getter
	private final String playerName;
	private final WebSocketSession session;
	private final ExecutorService virtualThreadExecutor;
	private final ReentrantLock sendLock = new ReentrantLock();
	@Getter @Setter
	private String playerId = null;
	@Getter @Setter
	private volatile boolean delayedClose = false;
	@Getter @Setter
	private volatile boolean closed = false;
	
	public String getSessionId() {
		return session.getId();
	}
	
	public Future<?> send(String message) {
		return virtualThreadExecutor.submit(() -> senderThread(message));
	}
	
	private void senderThread(String message) {
		try {
			sender(message);
		} catch (InterruptedException e) {
			log.error("Send to " + playerId + " interrupted");
		} catch (IOException e) {
			log.error("Error sending to " + playerId, e);
		}
	}
	
	private void sender(String message) throws InterruptedException, IOException {
		sendLock.lockInterruptibly();
		try {
			if (!closed && session.isOpen()) {
				session.sendMessage(new TextMessage(message));
			}
		} catch (IllegalStateException e) {
			log.error("WebSocket Illegal State, closing session");
			delayedClose = true;
		} finally {
			sendLock.unlock();
		}
		if (delayedClose && !closed) {
			close();
		}
	}

	public void close() throws IOException {
		closed = true;
		delayedClose = false;
		session.close();
	}
}
