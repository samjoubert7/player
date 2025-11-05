package samj.player.util;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class JacksonMapper extends ObjectMapper {
	private static final long serialVersionUID = 1L;

	public JacksonMapper() {
		super();
		JavaTimeModule module = new JavaTimeModule();
		super.registerModule(module);
	}
	
	public String toString(Object o) {
		try {
			return super.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			log.error("Error converting to String: " + e.getMessage());
			return Objects.toString(o);
		}
	}
}
