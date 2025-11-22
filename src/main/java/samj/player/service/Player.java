package samj.player.service;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/*
 * Use of the Player object is restricted to its package -> NO public modifiers.
 */

class Player implements Comparable<String> {
	private static final AtomicInteger PLAYER_ID = new AtomicInteger(0);
	private static final AtomicInteger DRONE_ID = new AtomicInteger(0);

	@Getter(AccessLevel.PACKAGE)
	@JsonProperty
	private final String id;

	@Getter(AccessLevel.PACKAGE)
	@JsonIgnore
	private final boolean auto; // autonomous, without user control. 

	@Getter(AccessLevel.PACKAGE)
	@JsonIgnore
	private final Board board;

	@Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String name;

	@Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
	@JsonIgnore
	private String sessionId;

	@Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
	@JsonProperty
	private int x;

	@Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
	@JsonProperty
	private int y;

	@Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
	@JsonProperty
	private int dx;

	@Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
	@JsonProperty
	private int dy;

	@Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
	@JsonIgnore
	private boolean moved = true; // update to UI at least once

	@Getter(AccessLevel.PACKAGE) @Setter(AccessLevel.PACKAGE)
	@JsonIgnore
	private int bounce = 0;

	Player(Board board, String name, boolean auto) throws InterruptedException {
		this.id = (auto) ? "d" + DRONE_ID.incrementAndGet() : "p" + PLAYER_ID.incrementAndGet();
		this.name = name;
		this.board = board;
		this.auto = auto;
		if (auto) {
			initDirection();
		}
	}

	void setXY(int x, int y) {
		this.x = x;
		this.y = y;
	}

	void key(boolean up, boolean down, boolean left, boolean right) {
		if (up) {
			if (left) {
				dx = (x > 0) ? -board.motionDiagPixels : 0;
				dy = (y > 0) ? -board.motionDiagPixels : 0;
			} else if (right) {
				dx = (x < board.maxX) ? board.motionDiagPixels : 0;
				dy = (y > 0) ? -board.motionDiagPixels : 0;
			} else {
				dx = 0;
				dy = (y > 0) ? -board.motionPixels : 0;
			}
		} else if (down) {
			if (left) {
				dx = (x > 0) ? -board.motionDiagPixels : 0;
				dy = (y < board.maxY) ? board.motionDiagPixels : 0;
			} else if (right) {
				dx = (x < board.maxX) ? board.motionDiagPixels : 0;
				dy = (y < board.maxY) ? board.motionDiagPixels : 0;
			} else {
				dx = 0;
				dy = (y < board.maxY) ? board.motionPixels : 0;
			}
		} else if (left) {
			dx = (x > 0) ? -board.motionPixels : 0;
			dy = 0;
		} else if (right) {
			dx = (x < board.maxX) ? board.motionPixels : 0;
			dy = 0;
		} else {
			dx = 0;
			dy = 0;
		}

	}

	boolean move() throws InterruptedException {
		moved = false;
		if (dx != 0) {
			x += dx;
			moved = true;
		}
		// Check x even if dx is now zero
		if (x < 0) {
			x = 0;
			if (finishedBouncing()) {
				dy = changeSubDirection(dy);
				dy = (dy != 0) ? board.motionDiagPixels : 0;
			}
			dx = (dy != 0) ? board.motionDiagPixels : board.motionPixels;
			moved = true;
		} else if (x > board.maxX) {
			x = board.maxX;
			if (finishedBouncing()) {
				dy = changeSubDirection(dy);
				dy = (dy != 0) ? board.motionDiagPixels : 0;
			}
			dx = (dy != 0) ? -board.motionDiagPixels : -board.motionPixels;
			moved = true;
		}
		if (dy != 0) {
			y += dy;
			moved = true;
		}
		// Check y even if dy is now zero
		if (y < 0) {
			y = 0;
			if (finishedBouncing()) {
				dx = changeSubDirection(dx);
				dx = (dx != 0) ? board.motionDiagPixels : 0;
			}
			dy = (dx != 0) ? board.motionDiagPixels : board.motionPixels;
			moved = true;
		} else if (y > board.height - board.playerSize - Math.abs(dy)) {
			y = board.height - board.playerSize;
			if (finishedBouncing()) {
				dx = changeSubDirection(dx);
				dx = (dx != 0) ? board.motionDiagPixels : 0;
			}
			dy = (dx != 0) ? -board.motionDiagPixels : -board.motionPixels;
			moved = true;
		}
		if (auto && dx == 0 && dy == 0) {
			changeDirection();
		}
		return moved;
	}
	
	boolean takeMoved() {
		if (!moved) {
			return false;
		}
		moved = false;
		return true;
	}

	private boolean finishedBouncing() throws InterruptedException {
		if (bounce >= board.maxBounce || board.randBool()) {
			bounce = 0;
			return true;
		}
		bounce++;
		return false;
	}

	private void initDirection() throws InterruptedException {
		byte randValue = board.randByte();
		dx = changeSubDirection(((randValue & 0xf) % 3) - 1);
		randValue = (byte) (randValue >> 4);
		dy = changeSubDirection(((randValue & 0xf) % 3) - 1);
		dx = dx * (dy != 0 ? board.motionDiagPixels : board.motionPixels);
		dy = dy * (dx != 0 ? board.motionDiagPixels : board.motionPixels);
	}

	private void changeDirection() throws InterruptedException {
		int newDx = changeSubDirection(dx);
		int newDy = changeSubDirection(dy);
		if (newDx == 0 && newDy == 0) {
			dx = -dx;
			dy = -dy;
		} else {
			int mult = (newDx != 0 && newDy != 0) ? board.motionDiagPixels : board.motionPixels;
			dx = newDx * mult;
			dy = newDy * mult;
		}
		bounce = 0;
	}

	private int changeSubDirection(int delta) throws InterruptedException {
		if (delta < 0) {
			return board.randBool() ? 1 : 0;
		}
		if (delta > 0) {
			return board.randBool() ? -1 : 0;
		}
		return board.randBool() ? -1 : 1;
	}

	void checkCollision(Player otherPlayer) throws InterruptedException {
		if (isCollision(otherPlayer)) {
			swapDirection(otherPlayer);
			int count = 5;
			do {
				move();
				otherPlayer.move();
			} while (isCollision(otherPlayer) && count-- >= 0);
		}
	}

	private boolean isCollision(Player otherPlayer) {
		int diffX = Math.abs(this.x - otherPlayer.x);
		if (diffX > board.checkPlayerSize) {
			return false;
		}
		int diffY = Math.abs(this.y - otherPlayer.y);
		return (diffY <= board.checkPlayerSize);
	}

	private void swapDirection(Player otherPlayer) throws InterruptedException {
		int xDiff = Math.abs(this.x - otherPlayer.x);
		int yDiff = Math.abs(this.y - otherPlayer.y);

		if (!this.auto || (this.dx == 0 && this.dy == 0)) {
			if (!otherPlayer.auto || (otherPlayer.dx == 0 && otherPlayer.dy == 0)) {
				otherPlayer.changeDirection();
			} else if (xDiff > yDiff) {
				otherPlayer.dx = -otherPlayer.dx;
			} else {
				otherPlayer.dy = -otherPlayer.dy;
			}
			otherPlayer.move();
			return;
		}
		if (otherPlayer.dx == 0 && otherPlayer.dy == 0) {
			if (xDiff > yDiff) {
				this.dx = -this.dx;
			} else {
				this.dy = -this.dy;
			}
			return;
		}
		
		if (xDiff > yDiff) {
			int temp = this.dx;
			this.dx = otherPlayer.dx;
			otherPlayer.dx = temp;
		} else {
			int temp = this.dy;
			this.dy = otherPlayer.dy;
			otherPlayer.dy = temp;
		}
	}

	void incBounce() {
		bounce++;
	}

	void clearBounce() {
		bounce = 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Player other = (Player) obj;
		return (id == other.id);
	}

	@Override
	public int compareTo(String otherId) {
		if (otherId == null) {
			return 1;
		}
		return id.compareTo(otherId);
	}

	String toDesc() {
		return id + '\t' + x + '\t' + y;
	}

	@Override
	public String toString() {
		return "Player [id=" + id + ", name=" + name + ", x=" + x + ", y=" + y + "]";
	}
}
