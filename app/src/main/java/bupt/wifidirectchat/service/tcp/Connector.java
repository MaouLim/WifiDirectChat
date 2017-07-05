package bupt.wifidirectchat.service.tcp;

/*
 * Created by Maou on 2017/7/5.
 */

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class Connector implements Runnable {

	public static final String TAG = "Connector";

	public static final int DEFAULT_REMOTE_PORT = 12345;
	public static final int DEFAULT_TIMEOUT     = 10000;

	private InetAddress remoteAddress = null;
	private int         remotePort    = DEFAULT_REMOTE_PORT;

	public Connector(InetAddress remoteAddress, int remotePort) {
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
	}

	public Connector(InetAddress remoteAddress) {
		this(remoteAddress, DEFAULT_REMOTE_PORT);
	}

	public void handleConnectionFailed(Socket socket, Throwable throwable, Object sender) {

		/* print the reason of the failure */
		throwable.printStackTrace();

		/* try to close the socket */
		try {
			if (null != socket && !socket.isClosed()) {
				socket.close();
			}
		}
		catch (IOException ex) {
			Log.e(TAG, "exception in handleConnectionFailed", ex);
		}
	}

	public abstract void handleConnectionEstablished(Socket socket, Object sender);

	@Override
	public void run() {
		Socket socket = new Socket();

		try {
			socket.connect(
				new InetSocketAddress(remoteAddress, remotePort),
				DEFAULT_TIMEOUT
			);

			handleConnectionEstablished(socket, this);
		}

		catch (IOException ex) {
			handleConnectionFailed(socket, ex, this);
		}
	}
}
