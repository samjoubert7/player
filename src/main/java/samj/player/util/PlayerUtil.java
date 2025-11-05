package samj.player.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class PlayerUtil {

	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
	// Avoid rude random names by excluding vowels.
	private static final byte[] NAME_CHARS_ARRAY = "bcdfghjklmnpqrstvwxyz".getBytes(StandardCharsets.UTF_8);

	public static String toHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xff;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars, StandardCharsets.UTF_8);
	}
	
	public static String toHex(ByteBuffer message, int len) {
        byte[] buf = new byte[len];
		message.duplicate().get(buf);
		return toHex(buf);
	}
	
	public static String genRandomUserName(byte[] randomSrc) {
		byte[] name = new byte[randomSrc.length];
		int value = (int) System.nanoTime();
		for (int i = 0; i < name.length; i++) {
			value += randomSrc[i];
			value &= 0xff;	// force positive value.
			value %= NAME_CHARS_ARRAY.length;
			name[i] = NAME_CHARS_ARRAY[value];
		}
		return new String(name);
	}
}
