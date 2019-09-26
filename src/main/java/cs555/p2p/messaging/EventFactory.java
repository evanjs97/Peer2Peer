package cs555.p2p.messaging;

import cs555.p2p.messaging.Event.Type;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Singleton Instance Design Pattern
 * Only 1 EventFactory can be created in any JVM instance
 */
public class EventFactory {
	private static EventFactory eventFactory;
	/**
	 * private constructor can only be called from this class
	 */
	private EventFactory(){}

	/**
	 * creates instance of EventFactory on class creation
	 * @return
	 */
	static {
		eventFactory = new EventFactory();
	}

	public static EventFactory getInstance() {
		return eventFactory;
	}

	/**
	 * Creates an event from any valid input byte array
	 * @param marshalledBytes the byte array received over the network
	 * @return an instance of an Event which is of the class type specified by the type present in marshalled bytes
	 * @throws IOException if there are issues with the input streams
	 */
	public Event getEvent(byte[] marshalledBytes) {
		ByteArrayInputStream baInputStream =
				new ByteArrayInputStream(marshalledBytes);
		DataInputStream din =
				new DataInputStream(new BufferedInputStream(baInputStream));
		try {
			Type type = Type.valueOf(din.readInt());
			Event e = null;
			switch (type) {
				case REGISTRATION_REQUEST:
					e = new RegisterRequest(din);
					break;
				case ID_NOT_AVAILABLE:
					e = new RegistrationFailure();
					break;
				case REGISTRATION_SUCCESS:
					e = new RegistrationSuccess(din);
					break;
				case ENTRY_REQUEST:
					e = new EntryRequest(din);
					break;
				default:
					System.err.println("Event of type " + type + " does not exist.");
					break;

			}
			baInputStream.close();
			din.close();
			return e;
		}catch(IOException ioe) {
			System.err.println("Received data does not constitute a valid event: No event number found");
			return null;
		}
	}

}