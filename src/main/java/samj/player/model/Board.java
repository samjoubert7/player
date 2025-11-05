package samj.player.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.apachecommons.CommonsLog;
import samj.player.util.PlayerRandom;

@CommonsLog
public class Board {

	public final int width = 600;
	public final int height = 400;
	public final int playerSize = 32;
	public final int maxX = width - playerSize - 1;
	public final int maxY = height - playerSize - 1;
	public final int checkPlayerSize = playerSize + 1;
	public final int maxBounce = 2;
	public final int motionPixels = 5;
	public final int motionDiagPixels = 3;

	private final Map<String, Player> playerMap = new HashMap<>();
	private final Map<String, Player> playerNameMap = new HashMap<>();
	private final List<Player> playerList = new ArrayList<>();
	// animationPlayerList is reserved for animate()
	private boolean playerListChanged = false;
	private final List<Player> animationPlayerList = new ArrayList<>();

	private final PlayerRandom rand = new PlayerRandom();
	
	public Player addPlayer(String name, boolean autonomous) throws InterruptedException {
		Player p;
		synchronized (playerMap) {
			if (playerNameMap.containsKey(name)) {
				throw new IllegalArgumentException("Name already in use: " + name);
			}
			p = new Player(this, name, autonomous);
			playerMap.put(p.getId(), p);
			playerNameMap.put(name, p);
			playerList.add(p);
			playerListChanged = true;
		}
		int x = rand.nextShort() % maxX;
		int y = rand.nextShort() % maxY;
		p.setXY(x, y);
		return p;
	}
	
	public void removePlayer(String id) {
		synchronized (playerMap) {
			Player p = playerMap.remove(id);
			if (p != null) {
				playerNameMap.remove(p.getName());
				int index = Collections.binarySearch(playerList, id);
				if (index >= 0) {
					playerList.remove(index);
					playerListChanged = true;
				}
			}
		}
	}

	public Player getPlayer(String id) {
		synchronized (playerMap) {
			return playerMap.get(id);
		}
	}

	public List<Player> getPlayerList() {
		synchronized (playerMap) {
			return new ArrayList<>(playerList);
		}
	}

	public List<Player> getAnimationPlayerList() {
		synchronized (playerMap) {
			if (playerListChanged) {
				playerListChanged = false;
				animationPlayerList.clear();
				animationPlayerList.addAll(playerList);
			}
			return animationPlayerList;
		}
	}

	public int getPlayerCount() {
		synchronized (playerMap) {
			return playerList.size();
		}
	}
	
	public int randInt() throws InterruptedException {
		return rand.nextInt();
	}

	public byte randByte() throws InterruptedException {
		return rand.nextByte();
	}
	
	public boolean randBool() throws InterruptedException {
		return rand.nextBool();
	}
	
	public byte[] randByteArray(int len) throws InterruptedException {
		byte[] randSrc = new byte[len];
		for (int i = 0; i < len; i++) {
			randSrc[i] = randByte();
		}
		return randSrc;
	}

	public List<String> animate() throws InterruptedException {
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
				if (isCollision(player1, player2)) {
					swapDirection(player1, player2);
					int count = 5;
					do {
						player1.move();
						player2.move();
					} while (isCollision(player1, player2) && count-- >= 0);
				}
			}
		}
		List<String> result = new ArrayList<>(animationList.size());
		for (int i = 0; i < animationList.size(); i++) {
			result.add(animationList.get(i).toDesc());
		}
		return result;
	}

	private boolean isCollision(Player player1, Player player2) {
		int diffX = Math.abs(player1.getX() - player2.getX());
		if (diffX > checkPlayerSize) {
			return false;
		}
		int diffY = Math.abs(player1.getY() - player2.getY());
		return (diffY <= checkPlayerSize);
	}

	private void swapDirection(Player player1, Player player2) {
		int xDiff = Math.abs(player1.getX() - player2.getX());
		int yDiff = Math.abs(player1.getY() - player2.getY());

		if (player1.getDx() == 0 && player1.getDy() == 0) {
			if (xDiff > yDiff) {
				player2.setDx(-player2.getDx());
			} else {
				player2.setDy(-player2.getDy());
			}
			return;
		}
		if (player2.getDx() == 0 && player2.getDy() == 0) {
			if (xDiff > yDiff) {
				player1.setDx(-player1.getDx());
			} else {
				player1.setDy(-player1.getDy());
			}
			return;
		}
		
		if (xDiff > yDiff) {
			int temp = player1.getDx();
			player1.setDx(player2.getDx());
			player2.setDx(temp);
		} else {
			int temp = player1.getDy();
			player1.setDy(player2.getDy());
			player2.setDy(temp);
		}
	}
}
