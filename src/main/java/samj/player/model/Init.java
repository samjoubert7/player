package samj.player.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class Init {

	private final int w;
	private final int h;
	private final String id;
	
	public Init(String id, Board board) {
		this.id = id;
		this.w = board.width;
		this.h = board.height;
	}
	
	@JsonIgnore
	public String toDesc() {
		return "w:" + w + "\th:" + h + "\tid:" + id;
	}
}
