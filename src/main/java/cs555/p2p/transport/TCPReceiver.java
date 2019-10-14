package cs555.p2p.transport;

import cs555.p2p.messaging.Event;
import cs555.p2p.messaging.EventFactory;
import cs555.p2p.node.Node;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class TCPReceiver implements Runnable {

	private Socket socket;
	private DataInputStream din;
	private Node node;
	private volatile boolean done;

	/**
	 * TCPReceiverThread creates new receiver thread instance
	 * @param socket the socket to receive messages over
	 * @throws IOException
	 */
	public TCPReceiver(Socket socket, Node node) throws IOException {
		this.node = node;
		this.socket = socket;
		this.din = new DataInputStream(socket.getInputStream());
		this.done = false;
	}

	public void close() {
		done = true;
	}

	/**
	 * run method
	 * reads from socket while not null
	 * send event to EventFactory after reading
	 */
	@Override
	public void run() {
		int dataLength = 0;
		while (socket != null && socket.isConnected() && !done) {
			try {
				dataLength = din.readInt();
				byte[] data = new byte[dataLength];
				din.readFully(data, 0, dataLength);
				Event event = EventFactory.getInstance().getEvent(data);
				node.onEvent(event, socket);
			} catch(EOFException eofe) {
				break;
			} catch(IOException ioe) {
				System.err.println("TCPReceiver: Error while reading from socket");
				ioe.printStackTrace();
			} catch(NegativeArraySizeException ne) {
				System.err.println("TCPReceiver: INVALID SIZE: " + dataLength);
				ne.printStackTrace();
			}
		}
		try {
			if(socket!=null) socket.close();
		} catch (Exception e) {

		}
	}
}