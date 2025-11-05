package samj.player;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class MockWebSocketSession implements WebSocketSession {

	private String id;
	private int maxDataSize = 8192;
	private final Map<String, Object> attributeMap;
	private final AtomicBoolean openStatus = new AtomicBoolean(true);
	private final List<String> messages = new ArrayList<>();
	
	public MockWebSocketSession(String id, String name) {
		this.id = id;
		this.attributeMap = Map.of("playerName", name);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public URI getUri() {
		return null;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributeMap;
	}

	@Override
	public Principal getPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAcceptedProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		this.maxDataSize = messageSizeLimit;
	}

	@Override
	public int getTextMessageSizeLimit() {
		return maxDataSize;
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendMessage(WebSocketMessage<?> message) throws IOException {
		if (message instanceof TextMessage textMessage) {
			String text = textMessage.getPayload();
			// log.info("websocket " + id + " message: " + text);
			synchronized (messages) {
				messages.add(text);
			}
		}
		try {
			Thread.sleep(Duration.ofMillis(1));
		} catch (InterruptedException e) {
			log.error("Interrupted", e);
		}
	}

	@Override
	public boolean isOpen() {
		return openStatus.get();
	}

	@Override
	public void close() throws IOException {
		if (openStatus.compareAndSet(true, false)) {
			log.info("MockWebSocket close");
		}
	}

	@Override
	public void close(CloseStatus status) throws IOException {
		log.info("MockWebSocket close status " + status);
		close();
	}

	public List<String> getMessages() {
		synchronized (messages) {
			List<String> list = new ArrayList<>(messages);
			messages.clear();
			return list;
		}
	}
}
