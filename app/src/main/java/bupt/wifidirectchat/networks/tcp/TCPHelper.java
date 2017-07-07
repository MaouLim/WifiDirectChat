package bupt.wifidirectchat.networks.tcp;

/*
 * Created by Maou on 2017/7/6.
 */

import bupt.wifidirectchat.util.SimpleThreadPool;

/*
 * the TCPHelper is to help start the tcp components
 */
public class TCPHelper {

	public static void startListener(Listener listener) {
		SimpleThreadPool.execute(listener);
	}

	public static void startConnector(Connector connector) {
		SimpleThreadPool.execute(connector);
	}

	public static void startCommunicator(Communicator communicator) {
		SimpleThreadPool.execute(communicator);
	}
}
