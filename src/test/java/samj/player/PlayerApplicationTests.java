package samj.player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.socket.CloseStatus;

import samj.player.service.PlayerService;
import samj.player.util.JacksonMapper;
import samj.player.ws.PlayerWebSocketHandler;

@SpringBootTest
@ContextConfiguration(classes = TestWebSocketConfig.class)
class PlayerApplicationTests {

	@Autowired
	JacksonMapper mapper;

	@Autowired
	PlayerService playerService;

	@Autowired
	PlayerWebSocketHandler websocketHandler;

	@Test
	void contextLoads() throws Exception {
		basicTests();
		stressTest();
	}

	private void basicTests() throws Exception {
		String playerId = playerService.getFirstPlayerId();
		String text = playerService.getPlayerDesc(playerId);
		System.out.println("Player Id: " + playerId);
		System.out.println("Player Desc: " + text);
		
		text = playerService.getInit(playerId);
		System.out.println("BoardDesc: " + text);
	}

	private void stressTest() throws Exception {
		List<MockWebSocketSession> sessions = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			String id = "t-" + (i + 1);
			MockWebSocketSession session = new MockWebSocketSession(id, id);
			sessions.add(session);
			websocketHandler.afterConnectionEstablished(session);
		}
		Thread.sleep(Duration.ofSeconds(2));
		for (MockWebSocketSession session : sessions) {
			websocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);
		}
	}
}
