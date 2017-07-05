package bupt.wifidirectchat.service.tcp;

/*
 * Created by Maou on 2017/7/5.
 */

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class Listener implements Runnable {

	public static final String TAG = "Listener";

	public static final int DEFAULT_SERVER_PORT = 12345;
	public static final int DEFAULT_BACKLOG     = 4;
	public static final int DEFAULT_TIMEOUT     = 1000; /* MS */

	private ServerSocket serverSocket = null;
	private boolean      available    = false;

	public Listener(int localPort) {
		try {
			this.serverSocket = new ServerSocket(localPort, DEFAULT_BACKLOG);
			this.serverSocket.setSoTimeout(DEFAULT_TIMEOUT);
			this.available = true;
		}
		catch (IOException ex) {
			Log.e(TAG, "exception in Listener ctor", ex);
		}
	}

	public Listener() { this(DEFAULT_SERVER_PORT); }

	public void close() {
		available = false;

		try {
			if (null != serverSocket && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		}
		catch (IOException ex) {
			Log.e(TAG, "exception in close", ex);
		}
	}

	public abstract void handleConnectionEstablished(Socket socket, Object sender);

	@Override
	public void run() {

		while (available) {
			try {
				handleConnectionEstablished(serverSocket.accept(), this);
			}

			catch (IOException ex) {
				/* timeout to check out available */
			}
		}

		Log.d(TAG, "listener finalized.");
	}
}
