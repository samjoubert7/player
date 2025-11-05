package samj.player.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import samj.player.model.Board;
import samj.player.model.Init;
import samj.player.model.Player;
import samj.player.util.JacksonMapper;
import samj.player.util.PlayerUtil;
import samj.player.ws.PlayerWebSocketHandler;
import samj.player.ws.PlayerWebSocketSession;

@RequiredArgsConstructor
@CommonsLog
public class PlayerService {
	private final ExecutorService virtualThreadExecutor;
	private final JacksonMapper mapper;
	private final Board board;

	private static final int DRONE_COUNT = 5;

	private static final int DESC_BUFFER_LEN = 256;

	// Wired up later to avoid circular dependencies.
	private PlayerWebSocketHandler webSocketHandler = null;

	private final ReentrantLock taskLock = new ReentrantLock();
	private volatile Future<?> task = null;

	public void setCustomWebSocketHandler(PlayerWebSocketHandler webSocketHandler) {
		log.info("SET CustomWebSocketHandler");
		this.webSocketHandler = webSocketHandler;
		try {
			for (int i = 0; i < DRONE_COUNT; i++) {
				board.addPlayer("!d@" + i + "!", true); // add self-moving drone
			}
		} catch (InterruptedException e) {
			log.error("InterruptedException during startup");
		}
	}

	public String genRandomUserName() throws InterruptedException {
		byte[] randSrc = board.randByteArray(3);
		return PlayerUtil.genRandomUserName(randSrc);
	}
	
	public String getInit(String id) {
		Init desc = new Init(id, board);
		return mapper.toString(desc);
	}

	public Player addPlayer(PlayerWebSocketSession session) throws InterruptedException {
		Player player = board.addPlayer(session.getPlayerName(), false);
		session.setPlayerId(player.getId());
		player.setSessionId(session.getSessionId());
		log.info("WebSocket " + session.getSessionId() + " Player id: " + player.getId());
		startTimerThread();
		return player;
	}

	public Player getPlayer(String id) {
		return board.getPlayer(id);
	}
	
	public List<Player> getPlayerList() {
		return board.getPlayerList();
	}

	public void removePlayer(String id) throws InterruptedException {
		board.removePlayer(id);
		int count = board.getPlayerCount();
		log.info("Removed player " + id + ", count => " + count);
		if (count <= DRONE_COUNT) {
			stopTimerThread();
		}
	}
	
	public void playerKey(String id, String keys) {
		Player p = board.getPlayer(id);
		if (p == null) {
			return;
		}
		boolean up = keys.contains("U");
		boolean down = keys.contains("D");
		boolean left = keys.contains("L");
		boolean right = keys.contains("R");
		p.key(up, down, left, right);
	}
	
	private void startTimerThread() throws InterruptedException {
		taskLock.lockInterruptibly();
		try {
			if (task == null) {
				log.info("START timer");
				task = virtualThreadExecutor.submit(this::timerThread);
			}
		} finally {
			taskLock.unlock();
		}
	}
	
	private void stopTimerThread() throws InterruptedException {
		taskLock.lockInterruptibly();
		try {
			Future<?> activeTask = task;
			if (activeTask != null) {
				log.info("CANCEL timer");
				activeTask.cancel(true);
				task = null;
			}
		} finally {
			taskLock.unlock();
		}
	}

	private void timerThread() {
		try {
			// Delay
			Thread.sleep(Duration.ofMillis(20));
			// Animate
			timerAction();
			// Reschedule
			taskLock.lockInterruptibly();
			try {
				if (task != null) {
					task = virtualThreadExecutor.submit(this::timerThread);
				}
			} finally {
				taskLock.unlock();
			}
		} catch (InterruptedException e) {
			log.info("Timer thread interrupted");
		} catch (Exception e) {
			log.error("Error in animation timer thread: " + e.getMessage(), e);
		}
	}

	private void timerAction() throws InterruptedException {
		StringBuilder sb = new StringBuilder(DESC_BUFFER_LEN);
		List<String> descList = board.animate();
		int maxDataSize = webSocketHandler.getMaxDataSize();
		for (String desc : descList) {
			// If adding this desc puts the length out of bounds
			if (sb.length() + desc.length() + 5 > maxDataSize) {
				sendUpdate(sb.toString());
				sb.setLength(0);
			}
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append(desc);
		}
		if (sb.length() > 0) {
			sendUpdate(sb.toString());
		}
	}
	
	private void sendUpdate(String content) {
		String text = "$p:" + content;
		webSocketHandler.sendAll(text);
		// log.info("Send: " + text);
	}
}
