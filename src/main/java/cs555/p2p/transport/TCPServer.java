package cs555.p2p.transport;

import cs555.p2p.node.Node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer implements Runnable{
	private ServerSocket serverSocket;
	private int port;
	private Node node;
	private volatile boolean done;

	/**
	 * TCPServerThread constructor creates new Server thread
	 * @param port to open server socket for, use '0' for automatic allocation
	 */
	public TCPServer(int port, Node node) {
		this.node = node;
		openServerSocket(port);
		this.done = false;
		this.port = this.serverSocket.getLocalPort();
	}

	/**
	 * openServerSocket opens ServerSocket over port
	 * @param port to open ServerSocket over, pass '0' to automatically allocate
	 */
	private void openServerSocket(int port) {
		try{
			this.serverSocket = new ServerSocket(port, 0, java.net.InetAddress.getLocalHost());
			return;
		}catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}

	public void stop() {
		this.done = true;
	}

	public InetAddress getInetAddress() {
		return serverSocket.getInetAddress();
	}

	public String getAddress() {
		return serverSocket.getInetAddress().getHostAddress();
	}

	public int getLocalPort() {
		return serverSocket.getLocalPort();
	}

	/**
	 * run method for thread
	 * blocks till connection made, then open TCPReceiverThread over that socket
	 */
	@Override
	public void run() {
		while (!done) {
			try {
				Socket socket = serverSocket.accept();
				new Thread(new TCPReceiver(socket, node)).start();
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage());
			}
		}
	}
}