package cs555.p2p.messaging;

import cs555.p2p.messaging.Event.Type;
import cs555.p2p.node.PeerNode;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton Instance Design Pattern
 * Only 1 EventFactory can be created in any JVM instance
 */
public class EventFactory {
	private static EventFactory eventFactory;
	private final static Logger LOGGER = Logger.getLogger(EventFactory.class.getName());
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
		LOGGER.setLevel(Level.INFO);
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
				case ENTRY_ACCEPTANCE_RESPONSE:
					e = new EntryAcceptanceResponse(din);
					break;
				case ENTRANCE_BROADCAST:
					e = new EntranceBroadcast(din);
					break;
				case TRAVERSE_REQUEST:
					e = new TraverseRequest(din);
					break;
				case EXIT_REQUEST:
					e = new ExitRequest(din);
					break;
				case EXIT_SUCCESS_RESPONSE:
					e = new ExitSuccessResponse();
					break;
				case STORE_REQUEST:
					e = new StoreRequest(din);
					break;
				case PEER_REQUEST:
					e = new PeerRequest(din);
					break;
				case PEER_RESPONSE:
					e = new PeerResponse(din);
					break;
				case STORE_RESPONSE:
					e = new StoreResponse(din);
					break;
				case FILE_DOWNLOAD_REQUEST:
					e = new FileDownloadRequest(din);
					break;
				case FILE_DOWNLOAD_RESPONSE:
					e = new FileDownloadResponse(din);
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