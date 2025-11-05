package samj.player.ws;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.socket.CloseStatus;

// Redefine web socket CloseStatus.
public enum WsCloseStatus {

	NORMAL(1000, "Normal"),
	GOING_AWAY(1001, "Going away"),
	PROTOCOL_ERROR(1002, "Protocol error"),
	NOT_ACCEPTABLE(1003, "Data format not acceptable"),
	RESERVED(1004, "Reserved"),
	NO_STATUS_CODE(1005, "No status code"),
	NO_CLOSE_FRAME(1006, "No close frame"),
	BAD_DATA(1007, "Bad data"),
	POLICY_VIOLATION(1008, "Policy violation"),
	TOO_BIG_TO_PROCESS(1009, "Too big to process"),
	REQUIRED_EXTENSION(1010, "Extension required"),
	SERVER_ERROR(1011, "Extension required"),
	SERVICE_RESTARTED(1012, "Extension required"),
	SERVICE_OVERLOAD(1013, "Extension required"),
	TLS_HANDSHAKE_FAILURE(1015, "Extension required");
	
	private final int code;
	private final String text;
	private static final Map<Integer, WsCloseStatus> MAP = genMap();

	private WsCloseStatus(int code, String text) {
		this.code = code;
		this.text = text;
	}
	
	public int getCode() {
		return code;
	}

	public String getDescription() {
		return text;
	}

	public static WsCloseStatus of(int code) {
		return MAP.get(code);
	}

	public static WsCloseStatus of(CloseStatus status) {
		return MAP.get(status.getCode());
	}

	public static String toString(CloseStatus status) {
		WsCloseStatus wsStatus = of(status.getCode());
		String reason = status.getReason();
		boolean hasReason = (reason != null && !reason.isBlank());
		return (wsStatus != null ? wsStatus.getDescription() : Integer.toString(status.getCode())) + 
				(hasReason ? " " + reason : "");
	}

	private static Map<Integer, WsCloseStatus> genMap() {
		Map<Integer, WsCloseStatus> newMap = new HashMap<>();
		for (WsCloseStatus status : values()) {
			newMap.put(status.code, status);
		}
		return Map.copyOf(newMap);
	}
}
