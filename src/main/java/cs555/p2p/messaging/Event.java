package cs555.p2p.messaging;

import java.io.IOException;
import java.util.HashMap;


public interface Event {

	Type getType();

	byte[] getBytes() throws IOException;

	enum Type {
		REGISTRATION_REQUEST(1),
		ID_NOT_AVAILABLE(2),
		REGISTRATION_SUCCESS(3),
		ENTRY_REQUEST(4);

		private int value;
		private static HashMap<Integer, Type> map = new HashMap<>();

		static {
			for (Type type : Type.values()) {
				map.put(type.value, type);
			}
		}

		public int getValue() {
			return this.value;
		}

		public static Type valueOf(int type) {
			return map.get(type);
		}

		Type(int value) {
			this.value = value;
		}

	}


}