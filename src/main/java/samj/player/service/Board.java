package samj.player.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.apachecommons.CommonsLog;
import samj.player.util.PlayerRandom;

/*
 * Use of the Board object is restricted to its package -> NO public modifiers.
 */
@CommonsLog
class Board {

	final int width = 600;
	final int height = 400;
	final int playerSize = 32;
	final int maxX = width - playerSize - 1;
	final int maxY = height - playerSize - 1;
	final int checkPlayerSize = playerSize + 1;
	final int maxBounce = 2;
	final int motionPixels = 5;
	final int motionDiagPixels = 3;

	private final Map<String, Player> playerMap = new HashMap<>();
	private final Map<String, Player> playerNameMap = new HashMap<>();
	private final List<Player> playerList = new ArrayList<>();
	// animationPlayerList is reserved for animate()
	private boolean playerListChanged = false;
	private final List<Player> animationPlayerList = new ArrayList<>();
	private final ReentrantLock boardLock = new ReentrantLock();

	private final PlayerRandom rand = new PlayerRandom();
	
	Player addPlayer(String name, boolean autonomous) throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			if (playerNameMap.containsKey(name)) {
				throw new IllegalArgumentException("Name already in use: " + name);
			}
			Player p = new Player(this, name, autonomous);
			playerMap.put(p.getId(), p);
			playerNameMap.put(name, p);
			playerList.add(p);
			playerListChanged = true;
			int x = rand.nextShort() % maxX;
			int y = rand.nextShort() % maxY;
			p.setXY(x, y);
			return p;
		} finally {
			boardLock.unlock();
		}
	}
	
	void removePlayer(String id) throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			Player p = playerMap.remove(id);
			if (p != null) {
				playerNameMap.remove(p.getName());
				int index = Collections.binarySearch(playerList, id);
				if (index >= 0) {
					playerList.remove(index);
					playerListChanged = true;
				}
			}
		} finally {
			boardLock.unlock();
		}
	}
	
	void playerKey(String playerId, boolean up, boolean down, boolean left, boolean right) throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			Player p = playerMap.get(playerId);
			if (p != null) {
				p.key(up, down, left, right);
			}
		} finally {
			boardLock.unlock();
		}
	}

	// Supply info object to avoid allocating a new one while looping through playerList
	boolean getPlayerInfo(String id, PlayerInfo info) throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			Player p = playerMap.get(id);
			return info.init(p);
		} finally {
			boardLock.unlock();
		}
	}

	Player getPlayer(String id) throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			return playerMap.get(id);
		} finally {
			boardLock.unlock();
		}
	}

	String getPlayerDesc(String id) throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			Player p = playerMap.get(id);
			return (p != null) ? p.toDesc() : null;
		} finally {
			boardLock.unlock();
		}
	}

	String getFirstPlayerId() throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			if (playerList.isEmpty()) {
				return null;
			}
			return playerList.getFirst().getId();
		} finally {
			boardLock.unlock();
		}
	}

	// Supply temporary info object to avoid allocating a new PlayerInfo object for each player.
	void iteratePlayer(StringBuilder sb, PlayerInfo info, PlayerFn fn) throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			for (Player p : playerList) {
				if (info.init(p)) {
					fn.process(sb, info);
				}
			}
		} finally {
			boardLock.unlock();
		}
	}

	List<Player> getAnimationPlayerList() throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			if (playerListChanged) {
				playerListChanged = false;
				animationPlayerList.clear();
				animationPlayerList.addAll(playerList);
			}
			return animationPlayerList;
		} finally {
			boardLock.unlock();
		}
	}

	int getPlayerCount() throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			return playerList.size();
		} finally {
			boardLock.unlock();
		}
	}

	List<String> animate() throws InterruptedException {
		boardLock.lockInterruptibly();
		try {
			List<Player> animationList = getAnimationPlayerList();
			// log.info("animate " + playerList.size());
			for (int i = 0; i < animationList.size(); i++) {
				animationList.get(i).move();
			}
			// Check for collisions
			for (int i = 0; i < animationList.size(); i++) {
				Player player1 = animationList.get(i);
				for (int j = i + 1; j < animationList.size(); j++) {
					Player player2 = animationList.get(j);
					player1.checkCollision(player2);
				}
			}
			List<String> result = new ArrayList<>(animationList.size());
			for (int i = 0; i < animationList.size(); i++) {
				result.add(animationList.get(i).toDesc());
			}
			return result;
		} finally {
			boardLock.unlock();
		}
	}
	
	int randInt() throws InterruptedException {
		return rand.nextInt();
	}

	byte randByte() throws InterruptedException {
		return rand.nextByte();
	}
	
	boolean randBool() throws InterruptedException {
		return rand.nextBool();
	}
	
	byte[] randByteArray(int len) throws InterruptedException {
		byte[] randSrc = new byte[len];
		for (int i = 0; i < len; i++) {
			randSrc[i] = randByte();
		}
		return randSrc;
	}
}
