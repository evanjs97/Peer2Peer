package cs555.p2p.node;

import cs555.p2p.messaging.Event;

import java.net.Socket;

public interface Node {
	void onEvent(Event event, Socket socket);

}
