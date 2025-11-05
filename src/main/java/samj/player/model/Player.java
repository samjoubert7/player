package samj.player.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

public class Player implements Comparable<String> {
	private static final AtomicInteger PLAYER_ID = new AtomicInteger(0);
	private static final AtomicInteger DRONE_ID = new AtomicInteger(0);

	@Getter @JsonProperty
	private final String id;

	@Getter @JsonIgnore
	private final boolean auto; // autonomous, without user control. 

	@Getter @JsonIgnore
	private final Board board;

	@Getter @Setter @JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String name;

	@Getter @Setter @JsonIgnore
	private String sessionId;

	@Getter @Setter @JsonProperty
	private int x;

	@Getter @Setter @JsonProperty
	private int y;

	@Getter @Setter @JsonProperty
	private int dx;

	@Getter @Setter @JsonProperty
	private int dy;

	@Getter @Setter @JsonIgnore
	private boolean moved = true; // update to UI at least once

	@Getter @Setter @JsonIgnore
	private int bounce = 0;

	public Player(Board board, String name, boolean auto) throws InterruptedException {
		this.id = (auto) ? "d" + DRONE_ID.incrementAndGet() : "p" + PLAYER_ID.incrementAndGet();
		this.name = name;
		this.board = board;
		this.auto = auto;
		if (auto) {
			initDirection();
		}
	}

	public void setXY(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void key(boolean up, boolean down, boolean left, boolean right) {
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

	public boolean move() throws InterruptedException {
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
	
	public boolean takeMoved() {
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


	public void incBounce() {
		bounce++;
	}

	public void clearBounce() {
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

	public String toDesc() {
		return id + '\t' + x + '\t' + y;
	}

	@Override
	public String toString() {
		return "Player [id=" + id + ", name=" + name + ", x=" + x + ", y=" + y + "]";
	}
}
