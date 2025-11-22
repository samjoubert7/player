package samj.player.service;

import lombok.Data;

@Data
public class PlayerInfo {
	private String id;
	private String name;
	private boolean auto;
	private boolean moved;
	
	public boolean init(Player p) {
		if (p != null) {
			this.id = p.getId();
			this.name = p.getName();
			this.auto = p.isAuto();
			this.moved = p.isMoved();
			return true;
		} else {
			this.id = null;
			this.name = null;
			this.auto = false;
			this.moved = false;
			return false;
		}
	}
}
